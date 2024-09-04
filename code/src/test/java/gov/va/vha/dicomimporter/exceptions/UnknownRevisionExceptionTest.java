package gov.va.vha.dicomimporter.exceptions;

import gov.va.vha.dicomimporter.RevisionSpecification;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for the InstanceValidationException class.
 */
public class UnknownRevisionExceptionTest {
    @DataProvider(name = "UnknownRevisionException")
    public static Object[][] primeNumbers() {
        return new Object[][] {
                {null, null, null},
                {null, RevisionSpecification.builder().build(), null},
                {null, RevisionSpecification.builder().withStringRepresentation("10").build(), null},
                {"", null, null},
                {"IDENTIFIER_1",
                        RevisionSpecification.builder().withStringRepresentation("-1").build(),
                        "IDENTIFIER_1"
                },
                {"IDENTIFIER_2",
                        RevisionSpecification.builder().withStringRepresentation("0").build(),
                        "IDENTIFIER_2"
                },
        };
    }

    @Test(dataProvider = "UnknownRevisionException")
    public void testMessage(
            final String identifier,
            final RevisionSpecification revisionSpecification,
            final String expectedMessageContent) {
        UnknownRevisionException urX = new UnknownRevisionException(identifier, revisionSpecification);
        Assert.assertNotNull(urX);
        Assert.assertNotNull(urX.getMessage());
        Assert.assertNotNull(urX.getLocalizedMessage());
        if (expectedMessageContent != null)
            Assert.assertTrue(urX.getMessage().contains(expectedMessageContent),
                    urX.getMessage() + " does not contain " + expectedMessageContent
            );
    }
}
