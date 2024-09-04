package gov.va.vha.dicomimporter.exceptions;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Unit tests for the InstanceValidationException class.
 */
public class InstanceValidationExceptionTest {
    @DataProvider(name = "TestStimulus")
    public static Object[][] stimulus() {
        return new Object[][] {
                {null, null, null},
                {null, Collections.emptyList(), null},
                {"", null, null},
                {"ContextX",
                        Collections.singletonList(new String[]{"propertyA", "ViolationA"}),
                        new String[]{"ContextX", "ViolationA"}
                },
                {"ContextXY",
                        Arrays.asList(
                                new String[]{"propertyB", "ViolationB"},
                                new String[]{"propertyC", "ViolationC"}
                        ),
                        new String[]{"ContextXY", "ViolationB", "ViolationC"}
                },
        };
    }

    @Test(dataProvider = "TestStimulus")
    public void testMessage(
            final String contextMessage,
            final List<String[]> constraintViolations,
            final String[] expectedMessageContent) {
        InstanceValidationException.Builder ivXBuilder = InstanceValidationException.builder();
        ivXBuilder.withContext(contextMessage);
        if (constraintViolations != null)
            constraintViolations.stream()
                    .forEach(constraintViolation -> ivXBuilder.withValidationFailure(constraintViolation[0], constraintViolation[1]));
        InstanceValidationException ivX = ivXBuilder.build();

        Assert.assertNotNull(ivX);
        Assert.assertNotNull(ivX.getMessage());
        Assert.assertNotNull(ivX.getLocalizedMessage());
        if (expectedMessageContent != null) {
            Arrays.stream(expectedMessageContent).forEach( expectedContentElement ->
                    Assert.assertTrue(ivX.getMessage().contains(expectedContentElement))
            );
        }
    }
}
