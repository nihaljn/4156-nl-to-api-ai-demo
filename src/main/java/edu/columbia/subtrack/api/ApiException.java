package edu.columbia.subtrack.api;

/** A failure with one of the codes in SPEC section 6; {@code field} is set only for VALIDATION_ERROR. */
public class ApiException extends RuntimeException {

    private final String code;
    private final String field;

    public ApiException(String code, String message, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}
