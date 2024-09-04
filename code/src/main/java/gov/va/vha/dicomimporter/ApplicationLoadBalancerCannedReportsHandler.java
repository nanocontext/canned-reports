package gov.va.vha.dicomimporter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import gov.va.vha.dicomimporter.exceptions.AbstractClientException;
import gov.va.vha.dicomimporter.exceptions.AbstractServiceException;
import gov.va.vha.dicomimporter.exceptions.BodyEncodingException;
import gov.va.vha.dicomimporter.exceptions.WrappedServiceException;
import gov.va.vha.dicomimporter.model.CanonicalDocument;
import gov.va.vha.dicomimporter.model.CanonicalRequest;
import gov.va.vha.dicomimporter.model.CanonicalResponse;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class ApplicationLoadBalancerCannedReportsHandler
    implements RequestHandler<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent>
{
    private final static String DEFAULT_BUCKET_NAME = "canned-reports";
    private final CannedReportsManager cannedReportsManager;
    private final Logger logger = LoggerFactory.getLogger(ApplicationLoadBalancerCannedReportsHandler.class);

    /**
     * The required (by Lambda framework) no-args constructor.
     */
    public ApplicationLoadBalancerCannedReportsHandler() {
        final String s3BucketName = ApplicationProperties.getSingleton().getProperty("S3_BUCKET_NAME", DEFAULT_BUCKET_NAME);
        this.cannedReportsManager = new CannedReportsManager(s3BucketName);
    }

    /**
     * This is the API for the template management. This method implements (or delegates) the following API methods:
     * POST /{identifier} - saves the body as an S3 item using the {identifier} as the key. Specific headers in the request
     *                      are extracted and recorded as metadata
     * GET [/{identifier}[/{revision}]] - responds with the content of the S3 item identified by {identifier} as the body.
     *                                  Specific metadata is included as headers in the response. The {revision} is optional,
     *                                  if included it must be in the format specified below.
     *                                  NOTE: when the identifier is omitted the response is a JSON formatted body including
     *                                  the metadata for all items in the S3 bucket. The metadata includes the same data returned
     *                                  from a HEAD request (though in the body as a JSON document)
     * PUT /{identifier} - replaces an existing S3 item with body of the request. The underlying S3 bucket has versioning enabled,
     *                   the new content will be saved as the latest revision. Specific headers will replace metadata values, otherwise
     *                   existing metadata will be copied to the new revision.
     * DELETE /{identifier}[/{revision}] - remove and existing S3 item identified by the identifier and revision, omission of the
     *                                     revision remove the most recent revision
     * HEAD [/identifier[/{revision}]] - returns the HTTP context including the following application defined headers:
     *                                 identifier, report-name, report-description, revision-count, revision
     *                                 content-type and content-length are also provided (derived from S3)
     *
     * HTTP Headers Retained as Metadata
     * The following HTTP headers are retained as S3 metadata:
     * "report-name" is stored as "x-amz-meta-report-name"
     * "report-description" is stored as "x-amz-meta-report-description"
     * "revision-count" is not stored as metadata but is calculated as needed
     * Note that HTTP headers are treated as case-insensitive, internally they are converted to lower-case.
     *
     * Revision Specification Format:
     * The revision must be in the following format:
     * "[-][1-9][0-9]*"
     * examples:
     * "0" means retrieve the oldest available revision
     * "2" means retrieve the third-oldest revision
     * "-1" means retrieve the most recent revision before the current revision
     * "-0" means retrieve the most recent revision
     *
     * @param event
     * @param context
     * @return
     */
    @Override
    public ApplicationLoadBalancerResponseEvent handleRequest(
            final ApplicationLoadBalancerRequestEvent event,
            final Context context)
    {
        logger.info("ApplicationLoadBalancerResponseEvent handleRequest({}, {})", event, context);

        try {
            CanonicalRequest canonicalRequest = parse(event);
            logger.debug("canonicalRequest is ({})", canonicalRequest);
            CanonicalResponse canonicalResponse = cannedReportsManager.handleRequest(canonicalRequest);
            logger.debug("canonicalResponse is ({})", canonicalResponse);
            return createResponse(canonicalRequest, canonicalResponse);
        } catch (AbstractClientException acX) {
            return createClientErrorResponse(acX);
        } catch (ParseException pX) {
            return createServerErrorResponse(new WrappedServiceException("handleRequest", pX));
        } catch (IOException ioX) {
            return createServerErrorResponse(new WrappedServiceException("handleRequest", ioX));
        }
    }

    private CanonicalRequest parse(ApplicationLoadBalancerRequestEvent event)
            throws AbstractClientException {
        // the full path may include:
        // / - the root e.g. "/"
        // /{identifier} - just an identifier e.g. "/655321"
        // /{identifier}/{revision-specification} - an identifier and a revision specification e.g. "/655321/-2"
        CanonicalRequest.Builder builder = CanonicalRequest.builder();
        builder.withMethod(event.getHttpMethod());

        final String path = event.getPath();
        final String[] pathElements = path.split("/");

        if (pathElements.length > 0)
            builder.withIdentifier(pathElements[0]);
        if (pathElements.length > 1)
            builder.withRevisionSpecification(pathElements[1]);

        // grab all the headers we may be interested in
        final Map<String, String> headers = event.getHeaders();
        builder.withName(headers.get(CannedReportsManager.HTTP_HEADER_REPORT_NAME));
        builder.withDescription(headers.get(CannedReportsManager.HTTP_HEADER_REPORT_DESCRIPTION));
        builder.withContentType(headers.get(HttpHeaders.CONTENT_TYPE));
        builder.withContentLength(headers.get(HttpHeaders.CONTENT_LENGTH) != null ? Integer.valueOf(headers.get(HttpHeaders.CONTENT_LENGTH)) : null);
        builder.withAuthorization(headers.get(HttpHeaders.AUTHORIZATION));
        try {
            builder.withBody(new StringInputStream(event.getBody()));
        } catch (UnsupportedEncodingException e) {
            throw new BodyEncodingException();
        }
        builder.withBodyIsBase64Encoded(event.getIsBase64Encoded());

        return builder.build();
    }

    private ApplicationLoadBalancerResponseEvent createResponse(
            final CanonicalRequest canonicalRequest,
            final CanonicalResponse canonicalResponse) throws IOException {
        logger.info("createResponse({}, {})", canonicalRequest, canonicalResponse);

        ApplicationLoadBalancerResponseEvent response = new ApplicationLoadBalancerResponseEvent();

        // default the status code and description, method specific handling may override these values
        response.setStatusCode(canonicalResponse.getResult().getHttpResponseCode());
        response.setStatusDescription(canonicalResponse.getResult().getHttpResponseDescription());
        logger.info("createResponse(...), (partial) response is [{}]", response);

        Map<String, String> headers = new HashMap<>();
        // the request method informs the format of the response
        switch (canonicalRequest.getMethod().toUpperCase()) {
            case "POST":
            case "PUT":
            case "DELETE":
            case "HEAD":
                // POST, PUT, DELETE and HEAD can work on only one document and have no body
                CanonicalDocument document = canonicalResponse.getReports().get(0);
                headers.put(CannedReportsManager.HTTP_HEADER_REPORT_NAME, document.getName());
                headers.put(CannedReportsManager.HTTP_HEADER_REPORT_DESCRIPTION, document.getDescription());
                headers.put(CannedReportsManager.HTTP_HEADER_REPORT_IDENTIFIER, document.getIdentifier());
                if (document.getRevision() != null)
                    headers.put(CannedReportsManager.HTTP_HEADER_REPORT_REVISION, document.getRevision().toString());
                response.setHeaders(headers);
                break;
            case "GET":
                if (canonicalRequest.getIdentifier() != null && !canonicalRequest.getIdentifier().isEmpty()) {
                    if (canonicalResponse.getReports().size() == 0) {
                        headers.put(CannedReportsManager.HTTP_HEADER_REPORT_IDENTIFIER, canonicalRequest.getIdentifier());
                        response.setHeaders(headers);
                        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                        response.setStatusDescription("NOT FOUND");
                    } else {
                        CanonicalDocument getDocument = canonicalResponse.getReports().get(0);

                        headers.put(CannedReportsManager.HTTP_HEADER_REPORT_NAME, getDocument.getName());
                        headers.put(CannedReportsManager.HTTP_HEADER_REPORT_DESCRIPTION, getDocument.getDescription());
                        headers.put(CannedReportsManager.HTTP_HEADER_REPORT_IDENTIFIER, getDocument.getIdentifier());
                        if (getDocument.getRevision() != null)
                            headers.put(CannedReportsManager.HTTP_HEADER_REPORT_REVISION, getDocument.getRevision().toString());
                        headers.put(HttpHeaders.CONTENT_TYPE, getDocument.getContentType());
                        headers.put(HttpHeaders.CONTENT_LENGTH, getDocument.getContentLength().toString());
                        response.setHeaders(headers);
                        response.setBody(getDocument.getBody());
                    }
                } else {
                    // if there is more than one document then the response is formatted as a JSON document
                    // for MVP, this implies that there is no document body in the documents within the CanonicalResponse
                    // because the GET request to populate the dropdown (i.e. just need the identifier, name, and description)
                    // Note that this will require encoding of the document body if those are to be included in the future
                    headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
                    response.setHeaders(headers);
                    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                    try {
                        final String json = ow.writeValueAsString(canonicalResponse);
                        headers.put(HttpHeaders.CONTENT_LENGTH, Integer.toString(json.length()));
                        response.setBody(json);
                    } catch (JsonProcessingException jpX) {
                        response = createServerErrorResponse(new WrappedServiceException("Unable to serialize document descriptions", jpX));
                    }
                }

                break;
        }

        logger.info("createResponse(...), returning response [{}]", response);
        return response;
    }

    private ApplicationLoadBalancerResponseEvent createClientErrorResponse(AbstractClientException acX) {
        ApplicationLoadBalancerResponseEvent result = new ApplicationLoadBalancerResponseEvent();
        result.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        result.setStatusDescription("Bad Request");
        result.setBody(acX.getMessage());
        return result;
    }

    private ApplicationLoadBalancerResponseEvent createServerErrorResponse(AbstractServiceException asX) {
        ApplicationLoadBalancerResponseEvent result = new ApplicationLoadBalancerResponseEvent();
        result.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        result.setStatusDescription("Internal Server Error");
        result.setBody(asX.getMessage());
        return result;
    }

}

/**
 * An ELB request looks something like this:
 * {
 *     "requestContext": {
 *         "elb": {
 *             "targetGroupArn": "arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/lambda-279XGJDqGZ5rsrHC2Fjr/49e9d65c45c6791a"
 *         }
 *     },
 *     "httpMethod": "GET",
 *     "path": "/lambda",
 *     "queryStringParameters": {
 *         "query": "1234ABCD"
 *     },
 *     "headers": {
 *         "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng",
 *         "accept-encoding":"gzip",
 *         "accept-language":"en-US,en;q=0.9",
 *          "connection":"keep-alive",
 *          "host":"lambda-alb-123578498.us-east-1.elb.amazonaws.com",
 *          "upgrade-insecure-requests":"1",
 *          "user-agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36",
 *          "x-amzn-trace-id":"Root=1-5c536348-3d683b8b04734faae651f476",
 *          "x-forwarded-for":"72.12.164.125",
 *          "x-forwarded-port":"80",
 *          "x-forwarded-proto":"http",
 *          "x-imforwards":"20"
 *          },
 *          "body":"",
 *          "isBase64Encoded":False
 *}
 */
