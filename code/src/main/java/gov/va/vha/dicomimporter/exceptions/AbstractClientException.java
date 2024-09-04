package gov.va.vha.dicomimporter.exceptions;

import org.apache.http.HttpStatus;

/**
 * The base class for all exceptions caused by a problem with a client request.
 * This includes invalid parameters, incorrect state, etc ...
 */
public abstract class AbstractClientException extends AbstractApplicationDefinedException {
    public int getHTTPResponseCode(){return HttpStatus.SC_BAD_REQUEST;};

    public AbstractClientException(final String message) {
        super(message);
    }
}
