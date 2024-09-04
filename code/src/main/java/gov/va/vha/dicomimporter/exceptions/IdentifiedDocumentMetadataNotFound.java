package gov.va.vha.dicomimporter.exceptions;

import org.apache.http.HttpStatus;

public class IdentifiedDocumentMetadataNotFound extends AbstractClientException {
    public int getHTTPResponseCode(){return HttpStatus.SC_NOT_FOUND;};

    private static String createMessage(final String identifier) {
        return "Document metadata cannot be found with identifier '" + identifier + "'.";
    }

    public IdentifiedDocumentMetadataNotFound(String identifier) {
        super(createMessage(identifier));
    }
}
