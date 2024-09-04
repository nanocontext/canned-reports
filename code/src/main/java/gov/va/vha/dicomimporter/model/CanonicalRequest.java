package gov.va.vha.dicomimporter.model;

import gov.va.vha.dicomimporter.RevisionSpecification;
import gov.va.vha.dicomimporter.exceptions.InstanceValidationException;

import java.io.InputStream;

public class CanonicalRequest {
    final String method;

    final String identifier;

    final RevisionSpecification revisionSpecification;

    final String name;
    final String description;
    final String contentType;
    final Integer contentLength;
    final String authorization;
    final InputStream body;
    final boolean bodyIsBase64Encoded;

    public CanonicalRequest(
            String method,
            String identifier, RevisionSpecification revisionSpecification,
            String name, String description,
            String contentType, Integer contentLength,
            String authorization,
            InputStream body, boolean bodyIsBase64Encoded) {
        this.method = method;
        this.identifier = identifier;
        this.revisionSpecification = revisionSpecification;
        this.name = name;
        this.description = description;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.authorization = authorization;
        this.body = body;
        this.bodyIsBase64Encoded = bodyIsBase64Encoded;
    }

    public String getMethod() {
        return method;
    }

    public String getIdentifier() {
        return identifier;
    }

    public RevisionSpecification getRevisionSpecification() {
        return revisionSpecification;
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

    public String getAuthorization() {
        return authorization;
    }

    public InputStream getBody() {
        return body;
    }

    public boolean isBodyIsBase64Encoded() {
        return bodyIsBase64Encoded;
    }

    @Override
    public String toString() {
        return "CanonicalRequest{" +
                "method='" + method + '\'' +
                ", identifier='" + identifier + '\'' +
                ", revisionSpecification=" + revisionSpecification +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", contentType='" + contentType + '\'' +
                ", contentLength=" + contentLength +
                ", authorization='" + authorization + '\'' +
                ", body=" + (body == null ? "null" : "not null") +
                ", bodyIsBase64Encoded=" + bodyIsBase64Encoded +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String method;
        private String identifier;
        private String revisionSpecification;
        private String name;
        private String description;
        private String contentType;
        private Integer contentLength;
        private InputStream body;
        private boolean bodyIsBase64Encoded;
        private String authorization;

        public Builder withMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withRevisionSpecification(String revisionSpecification) {
            this.revisionSpecification = revisionSpecification;
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

        public Builder withBody(InputStream body) {
            this.body = body;
            return this;
        }

        public Builder withBodyIsBase64Encoded(boolean bodyIsBase64Encoded) {
            this.bodyIsBase64Encoded = bodyIsBase64Encoded;
            return this;
        }

        public Builder withAuthorization(String authorization) {
            this.authorization = authorization;
            return this;
        }

        public CanonicalRequest build() throws InstanceValidationException {
            InstanceValidationException.Builder validationExceptionBuilder = InstanceValidationException.builder();

            if (this.method == null) {
                validationExceptionBuilder.withValidationFailure("method", "Null value not permitted");
            } else {
                final String upperCaseMethod = this.method.toUpperCase();

                if ("DELETE".equals(upperCaseMethod) || "PUT".equals(upperCaseMethod) || "HEAD".equals(upperCaseMethod)) {
                    if (this.identifier == null || identifier.length() < 1) {
                        validationExceptionBuilder.withValidationFailure("identifier", "Minimum length not met");
                    }
                }
                if ("POST".equals(upperCaseMethod) || "PUT".equals(upperCaseMethod)) {
                    if (this.body == null) {
                        validationExceptionBuilder.withValidationFailure("body", "Null value not permitted");
                    }
                    if (this.contentLength != null && this.contentLength < 1) {
                        validationExceptionBuilder.withValidationFailure("contentLength", "Minimum value not met");
                    }
                }
            }
            if (validationExceptionBuilder.includesValidationFailures())
                throw validationExceptionBuilder.build();

            RevisionSpecification revisionSpecification = RevisionSpecification.builder().withStringRepresentation(this.revisionSpecification).build();

            return new CanonicalRequest(
                    method,
                    identifier, revisionSpecification,
                    name, description,
                    contentType, contentLength,
                    authorization,
                    body, bodyIsBase64Encoded
            );
        }
    }
}
