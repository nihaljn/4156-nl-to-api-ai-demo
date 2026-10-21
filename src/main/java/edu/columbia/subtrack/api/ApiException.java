package edu.columbia.subtrack.api;

/** A failure that maps to one of the error responses in SPEC.md section 6. */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final String field;

    /**
     * Creates an exception.
     *
     * @param code the error code
     * @param message a client-safe description
     * @param field the input field to blame, or null when no single field is
     */
    public ApiException(final ErrorCode code, final String message, final String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    /**
     * Returns the error code.
     *
     * @return the code
     */
    public ErrorCode getCode() {
        return code;
    }

    /**
     * Returns the field to blame.
     *
     * @return the field name, or null
     */
    public String getField() {
        return field;
    }
}
