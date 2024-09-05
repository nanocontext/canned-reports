package gov.va.vha.dicomimporter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import gov.va.vha.dicomimporter.exceptions.AbstractClientException;
import gov.va.vha.dicomimporter.model.CanonicalRequest;
import gov.va.vha.dicomimporter.model.CanonicalResponse;
import org.apache.http.HttpHeaders;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class ApplicationLoadBalancerCannedReportsHandlerTest {
    @DataProvider(name = "ValidParseData")
    public Object[][] validParseData() {
        return new Object[][]{
                {
                    createMockEvent(
                        "POST",
                        null,
                        createHeaders("CT Brain Normal", null, null, null, null),
                        "CONTENT_01")
                },
                {
                    createMockEvent(
                            "GET",
                            "/ed60ea47-5a16-41df-8a02-26f001d2d0e7",
                            null,
                            null)
                },
        };
    }

    @DataProvider(name = "InvalidParseData")
    public Object[][] invalidParseData() {
        return new Object[][] {
                // method is required
                {createMockEvent(null, null, null, null),},
        };
    }

    @Test(dataProvider="ValidParseData")
    public void testValidParse(final ApplicationLoadBalancerRequestEvent event) throws AbstractClientException {
        ApplicationLoadBalancerCannedReportsHandler subject = new ApplicationLoadBalancerCannedReportsHandler();

        CanonicalRequest canonicalRequest = subject.parse(event);

        Assert.assertNotNull(canonicalRequest);
    }

    @Test(dataProvider="ValidParseData", enabled = false)
    public void testInvalidParse(final ApplicationLoadBalancerRequestEvent event) throws AbstractClientException {
        ApplicationLoadBalancerCannedReportsHandler subject = new ApplicationLoadBalancerCannedReportsHandler();

        try {
            CanonicalRequest canonicalRequest = subject.parse(event);
            Assert.fail("Invalid ApplicationLoadBalancerRequestEvent [" + event + "] parsed successfully and should not have.");
        } catch (Exception x) {
            // expected behavior
        }
    }

    private ApplicationLoadBalancerRequestEvent createMockEvent(
            final String httpMethod,
            final String path,
            final Map<String, String> headers,
            final String body
    ) {
        ApplicationLoadBalancerRequestEvent result = Mockito.mock(ApplicationLoadBalancerRequestEvent.class);
        Mockito.when(result.getHttpMethod()).thenReturn(httpMethod);
        Mockito.when(result.getPath()).thenReturn(path);
        Mockito.when(result.getHeaders()).thenReturn(headers);

        Mockito.when(result.getBody()).thenReturn(body);

        return result;
    }

    private Context createMockContext() {
        Context result = Mockito.mock(Context.class);
        return result;
    }

    private Map<String, String> createHeaders(
            final String name, final String description,
            final String contentType, final Integer contentLength,
            final String authorization) {
        final Map<String, String> result = new HashMap<>();
        if (name != null)
            result.put(CannedReportsManager.HTTP_HEADER_REPORT_NAME, name);
        if (description != null)
            result.put(CannedReportsManager.HTTP_HEADER_REPORT_DESCRIPTION, description);
        if (contentType != null)
            result.put(HttpHeaders.CONTENT_TYPE, contentType);
        if (contentLength != null)
            result.put(HttpHeaders.CONTENT_LENGTH, contentLength.toString());
        if (authorization != null)
            result.put(HttpHeaders.AUTHORIZATION, authorization);

        return result;
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

}