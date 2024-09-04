package gov.va.vha.dicomimporter;

import java.io.InputStream;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.*;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.StringUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import gov.va.vha.dicomimporter.exceptions.*;
import gov.va.vha.dicomimporter.model.CanonicalDocument;
import gov.va.vha.dicomimporter.model.CanonicalRequest;
import gov.va.vha.dicomimporter.model.CanonicalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the capabilities for Canned Report management and availability.
 * Derivations of this class implement the specific API through which this is being called,
 * as of now that is only an ALB implementation but since ALB has significant content size
 * limitations additional derivations may be required.
 */
public class CannedReportsManager {
    public static final String PROPERTY_ENABLE_AUTHORIZATION = "enable_authorization";
    public static final String PROPERTY_AUTHORIZATION_USER_ROLE = "authorization_user_role";
    public static final String PROPERTY_AUTHORIZATION_MANAGER_ROLE = "authorization_manager_role";
    public static final String DEFAULT_REPORT_USER = "canned_report_user";
    public static final String DEFAULT_REPORT_MANAGER = "canned_report_manager";

    public static final String HTTP_HEADER_REPORT_NAME = "report-name";
    public static final String HTTP_HEADER_REPORT_DESCRIPTION = "report-description";
    public static final String HTTP_HEADER_REPORT_IDENTIFIER = "report-identifier";
    public static final String HTTP_HEADER_REPORT_REVISION = "report-revision";

    public static final String S3_METADATA_REPORT_NAME = "x-amz-meta-report-name";
    public static final String S3_METADATA_REPORT_DESCRIPTION = "x-amz-meta-report-description";

    private final AmazonS3 amazonS3;
    private final String s3BucketName;
    private final boolean authorizationCheckingEnabled;
    private final String userRoleName;
    private final String managerRoleName;

    private final Logger logger = LoggerFactory.getLogger(CannedReportsManager.class);

    /**
     * Constructor for typical production usage
     * @param s3BucketName
     */
    protected CannedReportsManager(final String s3BucketName) {
        this(AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build(), s3BucketName);
    }

    /**
     * A constructor providing the means to mock the AmazonS3 instance for testing
     * @param amazonS3
     * @param s3BucketName
     */
    protected CannedReportsManager(final AmazonS3 amazonS3, final String s3BucketName) {
        logger.info("CannedReportsManager({}, {})", amazonS3, s3BucketName);

        if (StringUtils.isNullOrEmpty(s3BucketName))
            throw new InvalidParameterException("'s3BucketName' must not be null or empty");
        if (amazonS3 == null)
            throw new InvalidParameterException("amazonS3 must not be null");
        this.amazonS3 = amazonS3;
        this.s3BucketName = s3BucketName;

        // assure that the bucket exists (find it or create it)
        Bucket amazonS3Bucket = amazonS3.listBuckets().stream()
                .filter(bucket -> s3BucketName.equals(bucket.getName()))
                .findFirst()
                .orElse(null);
        if (amazonS3Bucket == null) {
            amazonS3Bucket = amazonS3.createBucket(s3BucketName);
            logger.info("created Bucket({})", amazonS3Bucket);
        }
        authorizationCheckingEnabled = Boolean.valueOf(
                ApplicationProperties.getSingleton().getProperty(PROPERTY_ENABLE_AUTHORIZATION, "false")
        );
        userRoleName = ApplicationProperties.getSingleton()
                .getProperty(PROPERTY_AUTHORIZATION_USER_ROLE, DEFAULT_REPORT_USER);
        managerRoleName = ApplicationProperties.getSingleton()
                .getProperty(PROPERTY_AUTHORIZATION_MANAGER_ROLE, DEFAULT_REPORT_MANAGER);

        logger.info("authorizationCheckingEnabled = [{}], userRoleName = [{}], managerRoleName = [{}]",
                authorizationCheckingEnabled, userRoleName, managerRoleName);
    }

    /**
     * This is the API for the template management. This method implements (or delegates) the following API methods:
     * POST /{identifier} - saves the body as an S3 item using the {identifier} as the key. Specific headers in the request
     * are extracted and recorded as metadata
     * GET [/{identifier}[/{revision}]] - responds with the content of the S3 item identified by {identifier} as the body.
     * Specific metadata is included as headers in the response. The {revision} is optional,
     * if included it must be in the format specified below.
     * NOTE: when the identifier is omitted the response is a JSON formatted body including
     * the metadata for all items in the S3 bucket. The metadata includes the same data returned
     * from a HEAD request (though in the body as a JSON document)
     * PUT /{identifier} - replaces an existing S3 item with body of the request. The underlying S3 bucket has versioning enabled,
     * the new content will be saved as the latest revision. Specific headers will replace metadata values, otherwise
     * existing metadata will be copied to the new revision.
     * DELETE /{identifier}[/{revision}] - remove and existing S3 item identified by the identifier and revision, omission of the
     * revision remove the most recent revision
     * HEAD [/identifier[/{revision}]] - returns the HTTP context including the following application defined headers:
     * identifier, report-name, report-description, revision-count, revision
     * content-type and content-length are also provided (derived from S3)
     * <p>
     * HTTP Headers Retained as Metadata
     * The following HTTP headers are retained as S3 metadata:
     * "report-name" is stored as "x-amz-meta-report-name"
     * "report-description" is stored as "x-amz-meta-report-description"
     * "revision-count" is not stored as metadata but is calculated as needed
     * Note that HTTP headers are treated as case-insensitive, internally they are converted to lower-case.
     * <p>
     * Revision Specification Format:
     * The revision must be in the following format:
     * "[-][1-9][0-9]*"
     * examples:
     * "0" means retrieve the current version
     * "6554321" means retrieve the version with that (internal S3) version number
     * "-1" means retrieve the most recent revision before the current version
     */
    protected CanonicalResponse handleRequest(final CanonicalRequest canonicalRequest) throws ParseException {
        logger.info("handleRequest({})", canonicalRequest);
        CanonicalResponse response = null;

        final List<String> roles = extractRolesFromAuthorization(canonicalRequest.getAuthorization());

        try {
            switch (canonicalRequest.getMethod().toUpperCase()) {
                case "POST":
                    if (authorizationCheckingEnabled && !roles.contains(this.managerRoleName)) {
                        response = CanonicalResponse.builder().forbiddenAccessException().build();
                    } else {
                        response = handlePost(
                                canonicalRequest.getName(), canonicalRequest.getDescription(),
                                canonicalRequest.getContentType(), canonicalRequest.getContentLength(),
                                canonicalRequest.getBody(), canonicalRequest.isBodyIsBase64Encoded()
                        );
                    }
                    logger.info("handleRequest({}) POST returning [{}]", canonicalRequest, response);
                    break;

                case "PUT":
                    if (authorizationCheckingEnabled && !roles.contains(this.managerRoleName)) {
                        response = CanonicalResponse.builder().forbiddenAccessException().build();
                    } else {
                        response = handlePut(canonicalRequest.getIdentifier(),
                                canonicalRequest.getName(), canonicalRequest.getDescription(),
                                canonicalRequest.getContentType(), canonicalRequest.getContentLength(),
                                canonicalRequest.getBody(), canonicalRequest.isBodyIsBase64Encoded()
                        );
                    }
                    logger.info("handleRequest({}) PUT returning [{}]", canonicalRequest, response);
                    break;

                case "GET":
                    if (authorizationCheckingEnabled && !(roles.contains(this.managerRoleName) || roles.contains(this.userRoleName))) {
                        response = CanonicalResponse.builder().forbiddenAccessException().build();
                    } else {
                        response = handleGet(canonicalRequest.getIdentifier(), canonicalRequest.getRevisionSpecification());
                    }
                    logger.info("handleRequest({}) GET returning [{}]", canonicalRequest, response);
                    break;

                case "DELETE":
                    if (authorizationCheckingEnabled && !roles.contains(this.managerRoleName)) {
                        response = CanonicalResponse.builder().forbiddenAccessException().build();
                    } else {
                        response = handleDelete(canonicalRequest.getIdentifier(), canonicalRequest.getRevisionSpecification());
                    }
                    logger.info("handleRequest({}) DELETE returning [{}]", canonicalRequest, response);
                    break;

                case "HEAD":
                    if (authorizationCheckingEnabled && !(roles.contains(this.managerRoleName) || roles.contains(this.userRoleName))) {
                        response = CanonicalResponse.builder().forbiddenAccessException().build();
                    } else {
                        response = handleHead(canonicalRequest.getIdentifier(), canonicalRequest.getRevisionSpecification());
                    }
                    logger.info("handleRequest({}) HEAD returning [{}]", canonicalRequest, response);
                    break;
            }
        } catch (AbstractApplicationDefinedException aadfX) {
            response = createExceptionResponse(aadfX);
            logger.info("handleRequest({}) caught exception [{}]", canonicalRequest, aadfX);
        }
        return response;
    }

    /**
     * To S3, POST is just a PUT with a new identifier.
     * Having handlePut and handlePost implements HTTP method semantics rather than S3.
     *
     * @param name
     * @param description
     * @param contentType
     * @param contentLength
     * @param body
     * @param bodyIsBase64Encoded
     * @return
     * @throws AbstractClientException
     * @throws AbstractServiceException
     */
    protected CanonicalResponse handlePost(
            String name, String description,
            String contentType, Integer contentLength,
            InputStream body, boolean bodyIsBase64Encoded)
        throws AbstractClientException, AbstractServiceException
    {
        final String identifier = UUID.randomUUID().toString();

        return internalHandlePostAndPut(identifier, name, description, contentType, contentLength, body, bodyIsBase64Encoded);
    }

    /**
     * Handle a PUT request. This checks for the existence of a document and if found then it
     * does a putObject with the same identifier (i.e. creates a new version). If not found
     * then it returns a 404 (not found).
     * Having handlePut and handlePost implements HTTP method semantics rather than S3.
     *
     * @param identifier
     * @param name
     * @param description
     * @param contentType
     * @param contentLength
     * @param body
     * @param bodyIsBase64Encoded
     * @return
     * @throws AbstractClientException
     * @throws AbstractServiceException
     */
    protected CanonicalResponse handlePut(
            String identifier,
            String name, String description,
            String contentType, Integer contentLength,
            InputStream body, boolean bodyIsBase64Encoded)
            throws AbstractClientException, AbstractServiceException {
        ObjectMetadata documentMetadata = amazonS3.getObjectMetadata(this.s3BucketName, identifier);

        if (documentMetadata != null) {
            return internalHandlePostAndPut(identifier, name, description, contentType, contentLength, body, bodyIsBase64Encoded);
        } else {
            return CanonicalResponse.builder().reportNotFoundException(null).build();
        }
    }

    /**
     *
     * @param identifier
     * @param name
     * @param description
     * @param contentType
     * @param contentLength
     * @param body
     * @param bodyIsBase64Encoded
     * @return
     * @throws AbstractClientException
     * @throws AbstractServiceException
     */
    private CanonicalResponse internalHandlePostAndPut(
            String identifier,
            String name, String description,
            String contentType, Integer contentLength,
            InputStream body, boolean bodyIsBase64Encoded)
            throws AbstractClientException, AbstractServiceException
    {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.addUserMetadata(S3_METADATA_REPORT_NAME, name);
            objectMetadata.addUserMetadata(S3_METADATA_REPORT_DESCRIPTION, description);
            // A sorta' hacky way to save the content type when it is transmitted as base64,
            // which, BTW, simply should not be done.
            if (contentType != null)
                if (bodyIsBase64Encoded && !contentType.endsWith("+base64"))
                    objectMetadata.setContentType(contentType + "+base64");
                else
                    objectMetadata.setContentType(contentType);
            if (contentLength != null)
                objectMetadata.setContentLength(contentLength);

            PutObjectResult result = amazonS3.putObject(s3BucketName, identifier, body, objectMetadata);

            if (result != null) {
                CanonicalDocument.Builder responseBuilder = CanonicalDocument.builder()
                        .withIdentifier(identifier)
                        .withRevision(0);
                Optional<ObjectMetadata> optionalResultMetadata = Optional.ofNullable(result.getMetadata());
                optionalResultMetadata.ifPresent(metadata -> {
                    responseBuilder.withContentType(metadata.getContentType());
                    responseBuilder.withContentLength((int)metadata.getContentLength());
                    responseBuilder.withName(metadata.getUserMetaDataOf(S3_METADATA_REPORT_NAME));
                    responseBuilder.withDescription(metadata.getUserMetaDataOf(S3_METADATA_REPORT_DESCRIPTION));
                });

                return CanonicalResponse.builder()
                        .successWithJSONBody(responseBuilder.build())
                        .build();
            } else {
                return CanonicalResponse.builder()
                        .serviceException(new WrappedServiceException("putObject response was null", null))
                        .build();
            }
        } catch (AmazonServiceException asX) {
            throw new UnrecoverableDependentInvocationException("AmazonS3.putObject", asX);
        }
    }

    /**
     * A GET can mean two things, one (if the identifier is not provided) is to GET the metadata for all the documents,
     * the other (if the identifier is provided) is to retrieve the content of a single document.
     *
     * @param identifier
     * @param revisionSpecification
     * @return
     * @throws AbstractClientException
     * @throws AbstractServiceException
     */
    protected CanonicalResponse handleGet(String identifier, RevisionSpecification revisionSpecification)
        throws AbstractClientException, AbstractServiceException
    {
        logger.info("handleGet({}, {})", identifier, revisionSpecification);

        if (identifier != null && identifier.length() > 0) {
            // get a single document requested by specifying the key
            return handleGetDocument(identifier, revisionSpecification);
        } else {
            // get the metadata of all of the documents
            return handleGetAllDocumentsMetadata();
        }
    }

    /**
     *
     * @param identifier
     * @param revisionSpecification
     * @return
     * @throws AbstractClientException
     * @throws AbstractServiceException
     */
    protected CanonicalResponse handleDelete(String identifier, RevisionSpecification revisionSpecification)
            throws AbstractClientException, AbstractServiceException
    {
        try {
            ObjectMetadata documentMetadata = amazonS3.getObjectMetadata(this.s3BucketName, identifier);

            if (documentMetadata != null) {
                // this must be done before the deleteObject because revision count calls S3
                CanonicalDocument canonicalDocument = CanonicalDocument.builder()
                        .withIdentifier(identifier)
                        .withObjectMetadata(documentMetadata)
                        .withRevision(getVersionCount(identifier))
                        .build();

                amazonS3.deleteObject(this.s3BucketName, identifier);

                return CanonicalResponse.builder().successWithJSONBody(canonicalDocument).build();
            } else {
                return CanonicalResponse.builder().reportNotFoundException(null).build();
            }
        } catch (SdkClientException sdkcx) {
            return CanonicalResponse.builder().serviceException(sdkcx).build();
        }
    }

    /**
     *
     * @param identifier
     * @param revisionSpecification
     * @return
     * @throws AbstractClientException
     * @throws AbstractServiceException
     */
    protected CanonicalResponse handleHead(String identifier, RevisionSpecification revisionSpecification)
            throws AbstractClientException, AbstractServiceException
    {
        try {
            ObjectMetadata documentMetadata = amazonS3.getObjectMetadata(this.s3BucketName, identifier);

            if (documentMetadata != null) {
                CanonicalDocument canonicalDocument = CanonicalDocument.builder()
                        .withIdentifier(identifier)
                        .withObjectMetadata(documentMetadata)
                        .withRevision(getVersionCount(identifier))
                        .build();

                return CanonicalResponse.builder()
                        .success()
                        .addDocument(canonicalDocument)
                        .build();
            } else {
                return CanonicalResponse.builder()
                        .reportNotFoundException(null)
                        .build();
            }
        } catch (SdkClientException sdkcx) {
            return CanonicalResponse.builder()
                    .serviceException(sdkcx)
                    .build();
        }
    }

    /**
     *
     * @param identifier
     * @param revisionSpecification
     * @return
     * @throws AbstractClientException
     * @throws AbstractServiceException
     */
    protected CanonicalResponse handleGetDocument(String identifier, RevisionSpecification revisionSpecification)
            throws AbstractClientException, AbstractServiceException
    {
        logger.info("handleGetDocument({}, {})", identifier, revisionSpecification);

        try {
            logger.debug("handleGetDocument({}, {}) getting document metadata", identifier, revisionSpecification);
            ObjectMetadata documentMetadata = amazonS3.getObjectMetadata(this.s3BucketName, identifier);
            if (documentMetadata != null) {
                logger.debug("handleGetDocument({}, {}) document metadata retrieved, getting object", identifier, revisionSpecification);

                S3Object s3Object = amazonS3.getObject(this.s3BucketName, identifier);
                if (s3Object != null) {
                    logger.debug("handleGetDocument({}, {}) object retrieved, building response", identifier, revisionSpecification);
                    CanonicalDocument canonicalDocument = CanonicalDocument.builder()
                            .withIdentifier(identifier)
                            .withObjectMetadata(documentMetadata)
                            .withRevision(getVersionCount(identifier))
                            .withBodyStream(s3Object.getObjectContent())
                            .build();

                    return new CanonicalResponse(
                            CanonicalResponse.Result.SUCCESS,
                            Collections.singletonList(canonicalDocument),
                            null
                    );
                } else {
                    throw new IdentifiedDocumentNotFound(identifier);
                }
            } else {
                throw new IdentifiedDocumentMetadataNotFound(identifier);
            }
        } catch(AmazonServiceException asX) {
            throw new WrappedServiceException("Getting Object or metadata", asX);
        }
    }

    /**
     * Get the metadata for all the objects in the S3 bucket
     * @return
     * @throws AbstractClientException
     * @throws AbstractServiceException
     */
    protected CanonicalResponse handleGetAllDocumentsMetadata()
            throws AbstractClientException, AbstractServiceException
    {
        logger.info("handleGetAllDocumentsMetadata()");

        // assume Success
        CanonicalResponse.Builder resultBuilder = CanonicalResponse.builder().success();
        try {
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
            listObjectsV2Request.setBucketName(this.s3BucketName);

            logger.debug("handleGetAllDocumentsMetadata(), calling listObjectsV2({})", listObjectsV2Request);
            ListObjectsV2Result objects = amazonS3.listObjectsV2(listObjectsV2Request);

            if (objects.getObjectSummaries() != null) {
                objects.getObjectSummaries().stream()
                        .map(s3ObjectSummary -> {
                                    ObjectMetadata metadata = amazonS3.getObjectMetadata(this.s3BucketName, s3ObjectSummary.getKey());
                                    return new KeyAndMetadata(s3ObjectSummary.getKey(), metadata);
                                }
                        )
                        .map(keyAndMetadata -> CanonicalDocument.builder()
                                .withIdentifier(keyAndMetadata.getKey())
                                .withName(keyAndMetadata.getMetadata().getUserMetaDataOf(S3_METADATA_REPORT_NAME))
                                .withDescription(keyAndMetadata.getMetadata().getUserMetaDataOf(S3_METADATA_REPORT_DESCRIPTION))
                                .withContentLength((int) keyAndMetadata.getMetadata().getContentLength())
                                .withContentType(keyAndMetadata.getMetadata().getContentType())
                                .build()
                        )
                        .forEach(canonicalDocument -> {
                            logger.debug("handleGetAllDocumentsMetadata(), adding ({}) to result", canonicalDocument);
                            resultBuilder.addDocument(canonicalDocument);
                        });
            }
            return resultBuilder.build();

        } catch(AmazonServiceException asX) {
            throw new WrappedServiceException("Getting Object metadata", asX);
        }
    }

    /**
     * The authorization header should follow the format "Authorization: Bearer <token>".
     * This code will accept either "Bearer <token>" or simply "<token>"
     * @param authorization
     * @return
     */
    private final String BEARER_PREFIX = "Bearer ";
    protected List<String> extractRolesFromAuthorization(final String authorization) throws ParseException {
        String[] roles = {};
        if (authorization != null) {
            String normalizedAuthorization = authorization.trim();
            if (normalizedAuthorization.startsWith(BEARER_PREFIX))
                normalizedAuthorization = normalizedAuthorization.substring(BEARER_PREFIX.length());

            SignedJWT signedJwt = SignedJWT.parse(normalizedAuthorization);
            if (signedJwt != null) {
                JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
                Object rawRoles = claims.getClaim("role");
                if (rawRoles instanceof String)
                    roles = ((String)rawRoles).split(",");
                if (rawRoles instanceof String[])
                    roles = (String[])rawRoles;
            }
        }

        return Arrays.asList(roles);
    }

    /**
     *
     * @param identifier
     * @return
     */
    protected int getVersionCount(final String identifier) {
        ListVersionsRequest listVersionsRequest = new ListVersionsRequest();
        listVersionsRequest.setBucketName(this.s3BucketName);
        listVersionsRequest.setPrefix(identifier);
        listVersionsRequest.setMaxResults(Integer.valueOf(1000));

        VersionListing versions = amazonS3.listVersions(listVersionsRequest);
        return versions == null || versions.getVersionSummaries() == null ? 0 : versions.getVersionSummaries().size();
    }

    /**
     *
     * @param identifier
     * @param revisionSpecification
     * @return
     * @throws UnknownRevisionException
     */
    protected String getVersionIdentifier(final String identifier, final RevisionSpecification revisionSpecification) throws UnknownRevisionException {
        ListVersionsRequest listVersionsRequest = new ListVersionsRequest();
        listVersionsRequest.setBucketName(this.s3BucketName);
        listVersionsRequest.setPrefix(identifier);
        listVersionsRequest.setMaxResults(Integer.valueOf(1000));

        VersionListing versions = amazonS3.listVersions(listVersionsRequest);
        int versionIndex = (versions.getVersionSummaries().size() - 1) + revisionSpecification.getValue();
        if (versionIndex >= 0 && versionIndex < versions.getVersionSummaries().size()) {
            return versions.getVersionSummaries().get(versionIndex).getVersionId();
        }
        throw new UnknownRevisionException(identifier, revisionSpecification);
    }

    /**
     * Create a CanonicalResponse given an application defined exception
     * @param aadfX
     * @return
     */
    private CanonicalResponse createExceptionResponse(AbstractApplicationDefinedException aadfX) {
        CanonicalResponse response = null;
        if (aadfX instanceof IdentifiedDocumentMetadataNotFound) {
            response = CanonicalResponse.builder()
                    .reportNotFoundException(aadfX)
                    .build();
        } else if (aadfX instanceof IdentifiedDocumentNotFound){
            response = CanonicalResponse.builder()
                    .reportNotFoundException(aadfX)
                    .build();
        } else if (aadfX instanceof AbstractClientException){
            response = CanonicalResponse.builder()
                    .genericBadRequestException(aadfX)
                    .build();
        } else if (aadfX instanceof AbstractServiceException){
            response = CanonicalResponse.builder()
                    .serviceException(aadfX)
                    .build();
        } else {
            response = CanonicalResponse.builder()
                    .serviceException(aadfX)
                    .build();
        }
        return response;
    }

    protected class KeyAndMetadata {
        final String key;
        final ObjectMetadata metadata;

        public KeyAndMetadata(String key, ObjectMetadata metadata) {
            this.key = key;
            this.metadata = metadata;
        }

        public String getKey() {
            return key;
        }

        public ObjectMetadata getMetadata() {
            return metadata;
        }
    }
}

