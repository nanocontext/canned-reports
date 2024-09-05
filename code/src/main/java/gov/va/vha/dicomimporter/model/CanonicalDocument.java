package gov.va.vha.dicomimporter.model;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;
import gov.va.vha.dicomimporter.CannedReportsManager;
import gov.va.vha.dicomimporter.exceptions.InstanceValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class CanonicalDocument {
    final String identifier;
    final Integer revision;
    final String name;
    final String description;
    final String contentType;
    final Integer contentLength;
    final String body;
    final InputStream bodyStream;
    final boolean bodyIsBase64Encoded;

    public CanonicalDocument(String identifier, Integer revision, String name, String description, String contentType, Integer contentLength, String body, InputStream bodyStream, boolean bodyIsBase64Encoded) {
        this.identifier = identifier;
        this.revision = revision;
        this.name = name;
        this.description = description;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.body = body;
        this.bodyStream = bodyStream;
        this.bodyIsBase64Encoded = bodyIsBase64Encoded;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Integer getRevision() {
        return revision;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContentType() {
        return contentType;
    }

    public Integer getContentLength() {
        return contentLength;
    }

    public String getBody() throws IOException {
        if (body != null) {
            return body;
        } else if (bodyStream != null){
            try (InputStreamReader isReader = new InputStreamReader(bodyStream)) {
                StringBuilder sb = new StringBuilder();
                char[] buffy = new char[2048];
                for (int charRead = isReader.read(buffy); charRead >= 0; charRead = isReader.read(buffy)) {
                    sb.append(Arrays.copyOfRange(buffy, 0, charRead));
                }
                return sb.toString();
            }
        } else {
            return null;
        }
    }

    public InputStream getBodyStream() throws UnsupportedEncodingException {
        if (bodyStream != null) {
            return bodyStream;
        } else if (body != null) {
            return new StringInputStream(body);
        } else {
            return null;
        }
    }

    public boolean isBodyIsBase64Encoded() {
        return bodyIsBase64Encoded;
    }

    @Override
    public String toString() {
        return "CanonicalDocument{" +
                "identifier='" + identifier + '\'' +
                ", revision=" + revision +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", contentType='" + contentType + '\'' +
                ", contentLength=" + contentLength +
                ", body='" + (body == null ? "null" : "not null") + '\'' +
                ", bodyStream=" + (bodyStream == null ? "null" : "not null") +
                ", bodyIsBase64Encoded=" + bodyIsBase64Encoded +
                "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String identifier;
        private Integer revision;
        private String name;
        private String description;
        private String contentType;
        private Integer contentLength;
        private String body;
        private InputStream bodyStream;
        private boolean bodyIsBase64Encoded;

        private Builder() {
        }

        public Builder withObjectMetadata(ObjectMetadata objectMetadata) {
            this.name = objectMetadata.getUserMetaDataOf(CannedReportsManager.HTTP_HEADER_REPORT_NAME);
            this.description = objectMetadata.getUserMetaDataOf(CannedReportsManager.HTTP_HEADER_REPORT_DESCRIPTION);
            this.contentLength = (int) objectMetadata.getContentLength();
            this.contentType = objectMetadata.getContentType();
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withRevision(Integer revision) {
            this.revision = revision;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withContentLength(Integer contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public Builder withBody(String body) {
            this.body = body;
            return this;
        }

        public Builder withBodyStream(InputStream bodyStream) {
            this.bodyStream = bodyStream;
            return this;
        }

        public Builder withBodyIsBase64Encoded(boolean bodyIsBase64Encoded) {
            this.bodyIsBase64Encoded = bodyIsBase64Encoded;
            return this;
        }

        public CanonicalDocument build() {
            InstanceValidationException.Builder ivXBuilder = InstanceValidationException.builder();
            ivXBuilder.withContext("CanonicalDocument");
            if (identifier == null || identifier.length() == 0)
                ivXBuilder.withValidationFailure("identifier", "Null or empty value not allowed");
            if (revision == null)
                ivXBuilder.withValidationFailure("revision", "Null value not allowed");
            if (name == null || name.length() == 0)
                ivXBuilder.withValidationFailure("name", "Null or empty value not allowed");

            return new CanonicalDocument(identifier, revision, name, description, contentType, contentLength, body, bodyStream, bodyIsBase64Encoded);
        }
    }
}
