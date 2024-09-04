package gov.va.vha.dicomimporter.exceptions;

import org.apache.http.HttpStatus;

public class WrappedServiceException extends AbstractServiceException {
    public int getHTTPResponseCode(){return HttpStatus.SC_BAD_GATEWAY;};

    public WrappedServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
