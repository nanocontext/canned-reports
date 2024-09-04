package gov.va.vha.dicomimporter.exceptions;

import gov.va.vha.dicomimporter.RevisionSpecification;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Unit tests for the InstanceValidationException class.
 */
public class UnrecoverableDependentInvocationExceptionTest {
    @DataProvider(name = "TestData")
    public static Object[][] testStimulus() {
        return new Object[][] {
                {null, null, null},
                {"CONTEXT_1", null, new String[]{"CONTEXT_1"}},
                {"", null, null},
                {null, new Throwable(), new String[]{"Throwable"}},
                {"CONTEXT_2",
                        new Throwable(),
                        new String[]{"Throwable", "CONTEXT_2"}
                },
        };
    }

    @Test(dataProvider = "TestData")
    public void test(
            final String context,
            final Throwable cause,
            final String[] expectedMessageContent) {
        UnrecoverableDependentInvocationException udiX = new UnrecoverableDependentInvocationException(context, cause);
        Assert.assertNotNull(udiX);
        Assert.assertNotNull(udiX.getMessage());
        Assert.assertNotNull(udiX.getLocalizedMessage());
        if (expectedMessageContent != null) {
            Arrays.stream(expectedMessageContent).forEach(expectedContentElement ->
                    Assert.assertTrue(udiX.getMessage().contains(expectedContentElement))
            );
        }
    }
}
