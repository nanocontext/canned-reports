package gov.va.vha.dicomimporter.exceptions;

import org.apache.http.HttpStatus;

public abstract class AbstractServiceException extends AbstractApplicationDefinedException {
    public int getHTTPResponseCode(){return HttpStatus.SC_INTERNAL_SERVER_ERROR;};

    public AbstractServiceException(String message) {
        super(message);
    }

    public AbstractServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
