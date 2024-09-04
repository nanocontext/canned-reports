package gov.va.vha.dicomimporter;

import java.security.InvalidParameterException;
import java.util.regex.Matcher;

public class RevisionSpecification {
    public final static String REGULAR_EXPRESSION_PATTERN = "(?<relative>[-+])?(?<value>[0-9]+)";
    public final static java.util.regex.Pattern REGEX_PATTERN = java.util.regex.Pattern.compile(REGULAR_EXPRESSION_PATTERN);
    private final boolean all;
    private final boolean relative;
    private final int value;

    public RevisionSpecification(boolean all, boolean relative, int value) {
        this.all = all;
        this.relative = relative;
        this.value = value;
    }

    public boolean isAll() {
        return all;
    }

    public boolean isRelative() {
        return relative;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return all ? "all" : ("" + value + (relative ? "(relative)" : ""));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean all = false;
        private boolean relative = false;
        private int value = 0;

        private Builder() {
        }

        public Builder withStringRepresentation(final String revisionSpecificationString) {
            if (revisionSpecificationString != null && revisionSpecificationString.length() > 0) {
                if ("ALL".equalsIgnoreCase(revisionSpecificationString)) {
                    this.all = true;
                } else {
                    this.all = false;
                    Matcher matcher = REGEX_PATTERN.matcher(revisionSpecificationString);
                    if (matcher.matches()) {
                        String relativeGroup = matcher.group("relative");
                        if (relativeGroup != null)
                            this.relative = true;
                        this.value = Integer.valueOf(revisionSpecificationString);
                    } else {
                        throw new InvalidParameterException("\"" + revisionSpecificationString + "\" does not follow pattern '" + REGULAR_EXPRESSION_PATTERN + "'");
                    }
                }
            } else {
                // default value is 0(relative), meaning the current revision
                this.relative = true;
                this.value = 0;
            }
            return this;
        }

        public RevisionSpecification build() {
            return new RevisionSpecification(all, relative, value);
        }
    }
}
