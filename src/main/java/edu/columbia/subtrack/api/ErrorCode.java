package edu.columbia.subtrack.api;

import org.springframework.http.HttpStatus;

/** The error codes of SPEC.md section 6, each with its HTTP status. */
public enum ErrorCode {
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    PLAN_NAME_TAKEN(HttpStatus.CONFLICT),
    ALREADY_SUBSCRIBED(HttpStatus.CONFLICT),
    ALREADY_CANCELED(HttpStatus.CONFLICT),
    IDEMPOTENCY_MISMATCH(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(final HttpStatus status) {
        this.status = status;
    }

    /**
     * Returns the HTTP status for this code.
     *
     * @return the status
     */
    public HttpStatus httpStatus() {
        return status;
    }
}
