package gov.va.vha.dicomimporter.exceptions;

import org.apache.http.HttpStatus;

public class BodyEncodingException extends AbstractClientException {
    public int getHTTPResponseCode(){return HttpStatus.SC_BAD_REQUEST;};

    public BodyEncodingException() {
        super("Unable to read body content because the character encoding was unknown.");
    }
}
