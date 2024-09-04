package gov.va.vha.dicomimporter.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class CanonicalResponse {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Result result;
    private Exception exception;
    private List<CanonicalDocument> reports;

    public CanonicalResponse(Result result, List<CanonicalDocument> reports, Exception exception) {
        this.result = result;
        this.reports = new ArrayList<>(reports);
        this.exception = exception;
    }

    public Result getResult() {
        return result;
    }

    public Exception getException() {
        return exception;
    }

    public List<CanonicalDocument> getReports() {
        return reports;
    }

    /**
     * An internal representation of the response code. The mapping to HttpStatus
     * is suggestive, not prescriptive.
     */
    public enum Result {
        SUCCESS(HttpStatus.SC_OK, "OK"),
        ACCEPTED(HttpStatus.SC_ACCEPTED, "ACCEPTED"),
        SERVICE_EXCEPTION(HttpStatus.SC_INTERNAL_SERVER_ERROR, "SERVER ERROR"),
        CLIENT_EXCEPTION(HttpStatus.SC_BAD_REQUEST, "BAD REQUEST"),
        NOT_FOUND(HttpStatus.SC_NOT_FOUND, "NOT FOUND"),
        FORBIDDEN(HttpStatus.SC_FORBIDDEN, "FORBIDDEN");

        private int httpResponseCode;
        private String httpResponseDescription;
        Result(int httpResponseCode, String httpResponseDescription) {
            this.httpResponseCode = httpResponseCode;
            this.httpResponseDescription = httpResponseDescription;
        }

        public int getHttpResponseCode() {
            return httpResponseCode;
        }

        public String getHttpResponseDescription() {
            return httpResponseDescription;
        }
    }

    @Override
    public String toString() {
        return "CanonicalResponse{" +
                "result=" + result +
                ", exception=" + exception +
                ", reports=" + (reports == null || reports.size()==0 ? 0 : reports.size()) +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Result result;
        private Exception exception;
        private List<CanonicalDocument> reports = new ArrayList<>();
        private String rawBody;

        private Builder() {
        }

        public Builder successWithReports(final List<CanonicalDocument> reports) {
            result = Result.SUCCESS;
            this.reports.addAll(reports);
            this.rawBody = null;
            this.exception = null;
            return this;
        }

        public Builder successWithJSONBody(final Object obj) {
            try {
                successWithBody(objectMapper.writeValueAsString(obj));
            } catch (JsonProcessingException jpX) {
                result = Result.SERVICE_EXCEPTION;
                this.exception = jpX;
            }
            return this;
        }

        public Builder successWithBody(final String body) {
            result = Result.SUCCESS;
            this.reports.clear();
            this.exception = null;
            this.rawBody = body;
            return this;
        }

        public Builder success() {
            result = Result.SUCCESS;
            this.reports.clear();
            this.exception = null;
            this.rawBody = null;
            return this;
        }

        public Builder addDocument(CanonicalDocument canonicalDocument) {
            this.reports.add(canonicalDocument);
            return this;
        }

        public Builder removeDocument(CanonicalDocument canonicalDocument) {
            this.reports.remove(canonicalDocument);
            return this;
        }

        public Builder withRawBody(final String rawBody) {
            this.rawBody = rawBody;
            return this;
        }

        public Builder serviceException(final Exception exception) {
            result = Result.SERVICE_EXCEPTION;
            this.exception = exception;
            this.reports.clear();
            return this;
        }

        public Builder genericBadRequestException(final Exception exception) {
            result = Result.CLIENT_EXCEPTION;
            this.exception = exception;
            this.reports.clear();
            return this;
        }

        public Builder reportNotFoundException(final Exception exception) {
            result = Result.NOT_FOUND;
            this.exception = exception;
            this.reports.clear();
            return this;
        }

        public Builder forbiddenAccessException() {
            result = Result.FORBIDDEN;
            this.exception = null;
            this.reports.clear();
            return this;
        }

        public CanonicalResponse build() {
            CanonicalResponse canonicalResponse = new CanonicalResponse(this.result, this.reports, this.exception);
            return canonicalResponse;
        }
    }
}
