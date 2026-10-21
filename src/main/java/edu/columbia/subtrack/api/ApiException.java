package edu.columbia.subtrack.api;

/** A failure with one of the codes in SPEC section 6; {@code field} is set only for VALIDATION_ERROR. */
public class ApiException extends RuntimeException {

    private final String code;
    private final String field;
    private final String existingSubscriptionId;

    public ApiException(String code, String message, String field) {
        this(code, message, field, null);
    }

    /** For ALREADY_SUBSCRIBED, whose body also carries {@code existingSubscriptionId} (SPEC 5.2). */
    public ApiException(String code, String message, String field, String existingSubscriptionId) {
        super(message);
        this.code = code;
        this.field = field;
        this.existingSubscriptionId = existingSubscriptionId;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }

    public String getExistingSubscriptionId() {
        return existingSubscriptionId;
    }
}
