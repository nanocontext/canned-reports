package gov.va.vha.dicomimporter.exceptions;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Unit tests for the InstanceValidationException class.
 */
public class WrappedServiceExceptionTest {

    @DataProvider(name = "TestData")
    public static Object[][] testStimulus() {
        return new Object[][] {
                {null, null},
                {"MESSAGE_1", null},
                {"", null},
                {null, new Throwable()},
                {"MESSAGE_2", new Throwable()},
        };
    }

    @Test(dataProvider = "TestData")
    public void test(final String message, final Throwable cause) {
        WrappedServiceException wsX = new WrappedServiceException(message, cause);
        Assert.assertNotNull(wsX);
    }
}
