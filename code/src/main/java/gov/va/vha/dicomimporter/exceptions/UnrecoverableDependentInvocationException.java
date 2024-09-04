package gov.va.vha.dicomimporter.exceptions;

import org.apache.http.HttpStatus;

public class UnrecoverableDependentInvocationException extends AbstractServiceException {
    public int getHTTPResponseCode(){return HttpStatus.SC_BAD_GATEWAY;};

    private static String createMessage(final String context, Throwable cause) {
        return "Remote invocation at [" + context + "] failed with exception [" + (cause == null ? "unknown" : cause.getClass().getName()) + "].";
    }
    public UnrecoverableDependentInvocationException(final String context, final Throwable cause) {
        super(createMessage(context, cause), cause);
    }
}
