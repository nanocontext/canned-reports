package gov.va.vha.dicomimporter;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RevisionSpecificationTest {
    @DataProvider(name = "ValidRevisionSpecification")
    public static Object[][] validRevisionSpecificationTestStimulus() {
        return new Object[][] {
                {null, false, true, 0},
                {"all", true, false, 0},
                {"0", false, false, 0},
                {"-0", false, true, 0},
                {"+0", false, true, 0},
                {"1", false, false, 1},
                {"+1", false, true, 1},
                {"-1", false, true, -1},
                {Integer.toString(Integer.MAX_VALUE), false, false, Integer.MAX_VALUE},  // not relative
                {"+" + Integer.toString(Integer.MAX_VALUE), false, true, Integer.MAX_VALUE},  // relative because it starts with a negative sign
                {Integer.toString(-2147483647), false, true, -2147483647},  // relative because it starts with a negative sign
        };
    }

    @DataProvider(name = "InvalidRevisionSpecification")
    public static Object[][] invalidRevisionSpecificationTestStimulus() {
        return new Object[][] {
                {"HelloWorld"},
                {"a1"},
                {Long.toString(Long.MAX_VALUE)},
                {Long.toString(Long.MIN_VALUE)},
        };
    }

    @Test(dataProvider = "ValidRevisionSpecification")
    public void testValidValues(final String validRevisionSpecification, final boolean expectedAll, final boolean expectedRelative, final int expectedValue) {
        RevisionSpecification revisionSpecification = RevisionSpecification.builder().withStringRepresentation(validRevisionSpecification).build();
        Assert.assertNotNull(revisionSpecification);
        Assert.assertEquals(revisionSpecification.isAll(), expectedAll);
        Assert.assertEquals(revisionSpecification.isRelative(), expectedRelative);
        Assert.assertEquals(revisionSpecification.getValue(), expectedValue);
    }

    @Test(dataProvider = "InvalidRevisionSpecification")
    public void testInvalidValues(final String invalidRevisionSpecification) {
        try {
            RevisionSpecification.builder().withStringRepresentation(invalidRevisionSpecification).build();
            Assert.fail("Invalid revision specification string [" + invalidRevisionSpecification + "] did not fail");
        } catch (Exception x) {
            // expected behavior
        }
    }

}
