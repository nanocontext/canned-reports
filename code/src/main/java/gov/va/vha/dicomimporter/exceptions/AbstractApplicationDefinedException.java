package gov.va.vha.dicomimporter.exceptions;

public abstract class AbstractApplicationDefinedException extends Exception {
    // each exception type should suggest a response code
    public abstract int getHTTPResponseCode();

    public AbstractApplicationDefinedException() {
    }

    public AbstractApplicationDefinedException(String message) {
        super(message);
    }

    public AbstractApplicationDefinedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbstractApplicationDefinedException(Throwable cause) {
        super(cause);
    }

    public AbstractApplicationDefinedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
