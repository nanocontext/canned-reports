package gov.va.vha.dicomimporter.model;

import com.amazonaws.util.StringInputStream;
import gov.va.vha.dicomimporter.exceptions.InstanceValidationException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class CanonicalRequestTest {
    @DataProvider(name = "ValidBuilderTestData")
    public static Object[][] validBuilderTestData() throws UnsupportedEncodingException {
        return new Object[][]{
                {"GET", null, null, null, null, null, false, null, null},
                {"GET", "", null, null, null, null, false, null, null},
                {"GET", "IDENTIFIER_001", null, null, null, null, false, null, null},
                {"GET", "IDENTIFIER_001", null, null, "-1", null, false, null, null},
                {"DELETE", "IDENTIFIER_001", null, null, null, null, false, null, null},
                {"DELETE", "IDENTIFIER_001", null, null, "0", null, false, null, null},
                {"POST", "IDENTIFIER_001", null, null, null, new StringInputStream("hello"), false, null, null},
                {"PUT", "IDENTIFIER_001", null, null, null, new StringInputStream("hello"), false, null, null},
                {"POST", "IDENTIFIER_001", null, null, null, new StringInputStream("hello"), true, "application/pdf", 655321},
                {"PUT", "IDENTIFIER_001", null, null, null, new StringInputStream("hello"), true, "application/pdf", 655321},
        };
    }

    @DataProvider(name = "InvalidBuilderTestData")
    public static Object[][] invalidBuilderTestData() {
        return new Object[][]{
                {"DELETE", null, null, null, null, null, false, null, null},
                {"DELETE", "", null, null, null, null, false, null, null},
                {"POST", null, null, null, null, null, false, null, null},
                {"PUT", null, null, null, null, null, false, null, null},
        };
    }

    @Test(dataProvider = "ValidBuilderTestData")
    public void testValidBuilder(
            final String method, final String identifier,
            final String name, final String description,
            final String revisionSpecification,
            final InputStream bodyStream, final boolean bodyIsBase64Encoded,
            final String contentType, final Integer contentLength) {
        CanonicalRequest.Builder builder = CanonicalRequest.builder();

        builder.withMethod(method);
        builder.withIdentifier(identifier);
        builder.withDescription(description);
        builder.withName(name);
        builder.withBody(bodyStream);
        builder.withBodyIsBase64Encoded(bodyIsBase64Encoded);
        builder.withRevisionSpecification(revisionSpecification);
        builder.withContentType(contentType);
        builder.withContentLength(contentLength);

        try {
            builder.build();
        } catch (InstanceValidationException ivX) {
            Assert.fail(ivX.getMessage());
        }
    }


    @Test(dataProvider = "InvalidBuilderTestData")
    public void testInvalidBuilder(
            final String method, final String identifier,
            final String name, final String description,
            final String revisionSpecification,
            final InputStream bodyStream, final boolean bodyIsBase64Encoded,
            final String contentType, final Integer contentLength) {
        CanonicalRequest.Builder builder = CanonicalRequest.builder();

        builder.withMethod(method);
        builder.withIdentifier(identifier);
        builder.withDescription(description);
        builder.withName(name);
        builder.withBody(bodyStream);
        builder.withBodyIsBase64Encoded(bodyIsBase64Encoded);
        builder.withRevisionSpecification(revisionSpecification);
        builder.withContentType(contentType);
        builder.withContentLength(contentLength);

        try {
            builder.build();
            Assert.fail("Building CanonicalRequest should have failed but did not");
        } catch (InstanceValidationException ivX) {
            // expected behavior
        }
    }
}
