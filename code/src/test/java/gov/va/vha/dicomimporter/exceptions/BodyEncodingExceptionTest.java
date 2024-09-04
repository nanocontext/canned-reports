package gov.va.vha.dicomimporter.exceptions;

import gov.va.vha.dicomimporter.RevisionSpecification;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for the BodyEncodingException class.
 */
public class BodyEncodingExceptionTest {
    public void test() {
        BodyEncodingException beX = new BodyEncodingException();
        Assert.assertNotNull(beX);
    }
}
