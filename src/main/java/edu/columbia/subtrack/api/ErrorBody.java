package edu.columbia.subtrack.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The body of every non-2xx response.
 *
 * @param error the error code name
 * @param message a client-safe description
 * @param field the field to blame, omitted when null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorBody(String error, String message, String field) {
}
