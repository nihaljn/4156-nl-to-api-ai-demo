package edu.columbia.subtrack.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/** The error body of SPEC section 2. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, String message, String field, String existingSubscriptionId) {

    public ErrorResponse(String error, String message, String field) {
        this(error, message, field, null);
    }
}
