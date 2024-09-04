package gov.va.vha.dicomimporter;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.StringInputStream;
import gov.va.vha.dicomimporter.exceptions.InstanceValidationException;
import gov.va.vha.dicomimporter.model.CanonicalDocument;
import gov.va.vha.dicomimporter.model.CanonicalRequest;
import gov.va.vha.dicomimporter.model.CanonicalResponse;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Unit tests for the InstanceValidationException class.
 */
public class CannedReportsManagerTest {

    @DataProvider(name = "TestGetData")
    public static Object[][] testGetData() throws InstanceValidationException {
        return new Object[][] {
                {
                        new String[]{"IDENTIFIER001"},
                        CanonicalRequest.builder().withMethod("GET").withIdentifier("IDENTIFIER001").build(),
                        HttpStatus.SC_OK
                },
                {
                        new String[]{"IDENTIFIER001", "IDENTIFIER002"},
                        CanonicalRequest.builder().withMethod("GET").withIdentifier("IDENTIFIER001").build(),
                        HttpStatus.SC_OK
                },
                {
                        new String[]{"IDENTIFIER001", "IDENTIFIER002"},
                        CanonicalRequest.builder().withMethod("GET").withIdentifier("IDENTIFIER002").build(),
                        HttpStatus.SC_OK
                },
                {
                        new String[]{"IDENTIFIER001", "IDENTIFIER002"},
                        CanonicalRequest.builder().withMethod("GET").withIdentifier("IDENTIFIER003").build(),
                        HttpStatus.SC_NOT_FOUND
                },
        };
    }

    @DataProvider(name = "TestHeadData")
    public static Object[][] testHeadData() throws InstanceValidationException {
        return new Object[][] {
                {
                        new String[]{"IDENTIFIER001"},
                        CanonicalRequest.builder().withMethod("HEAD").withIdentifier("IDENTIFIER001").build(),
                        HttpStatus.SC_OK
                },
                {
                        new String[]{"IDENTIFIER001", "IDENTIFIER002"},
                        CanonicalRequest.builder().withMethod("HEAD").withIdentifier("IDENTIFIER001").build(),
                        HttpStatus.SC_OK
                },
                {
                        new String[]{"IDENTIFIER001", "IDENTIFIER002"},
                        CanonicalRequest.builder().withMethod("HEAD").withIdentifier("IDENTIFIER002").build(),
                        HttpStatus.SC_OK
                },
                {
                        new String[]{"IDENTIFIER001", "IDENTIFIER002"},
                        CanonicalRequest.builder().withMethod("HEAD").withIdentifier("IDENTIFIER003").build(),
                        HttpStatus.SC_NOT_FOUND
                },
        };
    }

    @DataProvider(name = "TestDeleteData")
    public static Object[][] testDeleteData() throws InstanceValidationException {
        return new Object[][] {
                {
                        new String[]{"IDENTIFIER001", "IDENTIFIER002"},
                        CanonicalRequest.builder().withMethod("DELETE").withIdentifier("IDENTIFIER001").build(),
                        HttpStatus.SC_OK
                },
                {
                        new String[]{"IDENTIFIER001", "IDENTIFIER002"},
                        CanonicalRequest.builder().withMethod("DELETE").withIdentifier("IDENTIFIER003").build(),
                        HttpStatus.SC_NOT_FOUND
                },
        };
    }

    @DataProvider(name = "TestPostData")
    public static Object[][] testPostData() throws InstanceValidationException, UnsupportedEncodingException {
        return new Object[][] {
                {
                        new String[]{},
                        CanonicalRequest.builder()
                                .withMethod("POST")
                                .withIdentifier("IDENTIFIER001")
                                .withContentType("text/plain")
                                .withName("NAME001")
                                .withDescription("DESCRIPTION001")
                                .withBody(new StringInputStream("BODY_01"))
                                .build(),
                        HttpStatus.SC_OK
                },
        };
    }

    @Test(dataProvider = "TestGetData")
    public void testGet(final String[] objectIdentifiers, final CanonicalRequest request, final int expectedResponseCode) throws IOException, ParseException {
        String mockBucketName = "mock_bucket";

        AmazonS3 amazonS3Mock = createMockAmazonS3(mockBucketName, objectIdentifiers);

        CannedReportsManager subject = new CannedReportsManager(amazonS3Mock, mockBucketName) {};
        Assert.assertNotNull(subject, "Failed to create AbstractBaseTemplatesHandler (test subject)");

        CanonicalResponse response = subject.handleRequest(request);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getResult());
        Assert.assertEquals(response.getResult().getHttpResponseCode(), expectedResponseCode);
        if (expectedResponseCode == 200) {
            Assert.assertNotNull(response.getReports());
            Assert.assertEquals(response.getReports().size(), 1);
            CanonicalDocument report = response.getReports().get(0);
            Assert.assertNotNull(report);
            Assert.assertEquals(report.getIdentifier(), request.getIdentifier());
            Assert.assertEquals(report.getName(), "REPORT_" + request.getIdentifier());
            Assert.assertEquals(report.getDescription(), "DESCRIPTION_" + request.getIdentifier());
            Assert.assertEquals(report.getBody(), "CONTENT_" + request.getIdentifier());
            Assert.assertEquals(report.getContentType(), "text/plain");
        }
    }

    @Test(dataProvider = "TestHeadData")
    public void testHead(final String[] objectIdentifiers, final CanonicalRequest request, final int expectedResponseCode) throws UnsupportedEncodingException, ParseException {
        String mockBucketName = "mock_bucket";

        AmazonS3 amazonS3Mock = createMockAmazonS3(mockBucketName, objectIdentifiers);

        CannedReportsManager subject = new CannedReportsManager(amazonS3Mock, mockBucketName) {};
        Assert.assertNotNull(subject, "Failed to create AbstractBaseTemplatesHandler (test subject)");

        CanonicalResponse response = subject.handleRequest(request);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getResult());
        Assert.assertEquals(response.getResult().getHttpResponseCode(), expectedResponseCode);
        if (expectedResponseCode == 200) {
            Assert.assertNotNull(response.getReports());
            Assert.assertEquals(response.getReports().size(), 1);
            CanonicalDocument report = response.getReports().get(0);
            Assert.assertNotNull(report);
            Assert.assertEquals(report.getIdentifier(), request.getIdentifier());
            Assert.assertEquals(report.getName(), "REPORT_" + request.getIdentifier());
            Assert.assertEquals(report.getDescription(), "DESCRIPTION_" + request.getIdentifier());
        }
    }


    @Test(dataProvider = "TestDeleteData")
    public void testDelete(final String[] objectIdentifiers, final CanonicalRequest request, final int expectedResponseCode) throws UnsupportedEncodingException, ParseException {
        String mockBucketName = "mock_bucket";

        AmazonS3 amazonS3Mock = createMockAmazonS3(mockBucketName, objectIdentifiers);

        CannedReportsManager subject = new CannedReportsManager(amazonS3Mock, mockBucketName) {};
        Assert.assertNotNull(subject, "Failed to create AbstractBaseTemplatesHandler (test subject)");

        CanonicalResponse response = subject.handleRequest(request);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getResult());
        Assert.assertEquals(response.getResult().getHttpResponseCode(), expectedResponseCode);
    }

    @Test(dataProvider = "TestPostData")
    public void testPost(final String[] objectIdentifiers, final CanonicalRequest request, final int expectedResponseCode) throws UnsupportedEncodingException, ParseException {
        String mockBucketName = "mock_bucket";

        AmazonS3 amazonS3Mock = createMockAmazonS3(mockBucketName, objectIdentifiers);

        CannedReportsManager subject = new CannedReportsManager(amazonS3Mock, mockBucketName) {};
        Assert.assertNotNull(subject, "Failed to create AbstractBaseTemplatesHandler (test subject)");

        CanonicalResponse response = subject.handleRequest(request);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getResult());
        Assert.assertEquals(response.getResult().getHttpResponseCode(), expectedResponseCode);
    }

    @Test
    public void testListReports() throws UnsupportedEncodingException, InstanceValidationException, ParseException {
        String mockBucketName = "mock_bucket";
        String[] identifiers = new String[]{"IDENTIFIER01", "IDENTIFIER02", "IDENTIFIER03"};

        AmazonS3 amazonS3Mock = createMockAmazonS3(mockBucketName, identifiers);

        CannedReportsManager subject = new CannedReportsManager(amazonS3Mock, mockBucketName) {};
        Assert.assertNotNull(subject, "Failed to create AbstractBaseTemplatesHandler (test subject)");

        // a GET with no identifier in the path should result in a list of all the objects metadata
        CanonicalRequest request = CanonicalRequest.builder().withMethod("GET").build();

        CanonicalResponse response = subject.handleRequest(request);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getResult());
        Assert.assertEquals(response.getResult().getHttpResponseCode(), 200);
        Assert.assertNotNull(response.getReports());
        Assert.assertEquals(response.getReports().size(), identifiers.length);

        List<String> identifierAccountingList = new LinkedList<>(Arrays.asList(identifiers));
        for (int identifierIndex = 0; identifierIndex < identifiers.length; ++identifierIndex) {
            CanonicalDocument report = response.getReports().get(identifierIndex);
            Assert.assertNotNull(report);
            String actualIdentifier = report.getIdentifier();
            if (identifierAccountingList.remove(actualIdentifier)) {
                Assert.assertNotNull(report.getName());
                Assert.assertEquals(report.getName(), "REPORT_" + actualIdentifier);
                Assert.assertNotNull(report.getDescription());
                Assert.assertEquals(report.getDescription(), "DESCRIPTION_" + actualIdentifier);
            } else {
                Assert.fail("Found a report [" + actualIdentifier + "] in returned list that should not be there.");
            }
        };
    }

    /**
     * Create a mock AmazonS3 that will behave enough like the real thing for these tests
     * @param mockBucketName
     * @param identifiers
     * @return
     * @throws UnsupportedEncodingException
     */
    private AmazonS3 createMockAmazonS3(
            final String mockBucketName,
            final String[] identifiers
    ) {
        // create the AmazonS3 mock object, which is called by AbstractBaseTemplateHandler
        AmazonS3 amazonS3Mock = Mockito.mock(AmazonS3.class);
        Bucket mockBucket = Mockito.mock(Bucket.class);

        // mock the behavior regarding the one bucket
        Mockito.when(amazonS3Mock.listBuckets()).thenReturn(Collections.singletonList(mockBucket));
        Mockito.when(amazonS3Mock.createBucket(Mockito.anyString())).thenReturn(mockBucket);

        // mock the behavior of the objects in the bucket
        // first, create the object returned from a amazonS3Mock.listObjectsV2 call
        ListObjectsV2Result listObjectResult = new ListObjectsV2Result();
        Mockito.when(amazonS3Mock.listObjectsV2(Mockito.any(ListObjectsV2Request.class)))
                .thenReturn(listObjectResult);
        // second, create the objects and mock the behavior of the amazonS3Mock to return object results
        Arrays.stream(identifiers).forEach(identifier -> {
            final String objectContent = "CONTENT_" + identifier;

            // create the ObjectMetadata and mock its result from amazonS3Mock
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType("text/plain");
            objectMetadata.setContentLength(objectContent.length());
            objectMetadata.addUserMetadata(CannedReportsManager.S3_METADATA_REPORT_NAME, "REPORT_" + identifier);
            objectMetadata.addUserMetadata(CannedReportsManager.S3_METADATA_REPORT_DESCRIPTION, "DESCRIPTION_" + identifier);
            Mockito.when(amazonS3Mock.getObjectMetadata(mockBucketName, identifier)).thenReturn(objectMetadata);

            // create the Object and mock its result from amazonS3Mock
            S3Object s3Object = new S3Object();
            s3Object.setKey(identifier);
            s3Object.setBucketName(mockBucket.getName());
            try {
                s3Object.setObjectContent(new StringInputStream(objectContent));
            } catch (UnsupportedEncodingException e) {
                // ignore it
            }
            Mockito.when(amazonS3Mock.getObject(mockBucketName, identifier)).thenReturn(s3Object);

            // finally create object metadata and mock its return from amazonS3Mock
            S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
            s3ObjectSummary.setKey(identifier);
            s3ObjectSummary.setBucketName(mockBucketName);
            s3ObjectSummary.setSize(objectContent.length());
            listObjectResult.getObjectSummaries().add(s3ObjectSummary);
        });

        // mock successful putObject behavior
        PutObjectResult putObjectResult = new PutObjectResult();
        Mockito.when(amazonS3Mock.putObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(putObjectResult);

        return amazonS3Mock;
    }
}
