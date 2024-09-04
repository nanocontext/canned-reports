package gov.va.vha.dicomimporter.exceptions;

import gov.va.vha.dicomimporter.RevisionSpecification;
import org.apache.http.HttpStatus;

public class UnknownRevisionException extends AbstractClientException {
    public int getHTTPResponseCode(){return HttpStatus.SC_NOT_FOUND;};

    private static String createMessage(final String identifier, final RevisionSpecification revisionSpecification) {
        return "Revision Specification [" + revisionSpecification + "] does not specify a revision for [" + identifier + "]";
    }

    public UnknownRevisionException(final String identifier, final RevisionSpecification revisionSpecification) {
        super(createMessage(identifier, revisionSpecification));
    }
}
