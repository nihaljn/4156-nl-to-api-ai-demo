package edu.columbia.subtrack.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns exceptions into the error body of SPEC.md section 2. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Handles a deliberate API failure.
     *
     * @param ex the failure
     * @return the error response
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> handleApi(final ApiException ex) {
        return respond(ex.getCode(), ex.getMessage(), ex.getField());
    }

    /**
     * Handles a path or method that matches no endpoint.
     *
     * @param ex the routing failure
     * @return the NOT_FOUND response
     */
    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorBody> handleNoEndpoint(final Exception ex) {
        return respond(ErrorCode.NOT_FOUND, "No endpoint matches this request", null);
    }

    /**
     * Handles anything unexpected without leaking details.
     *
     * @param ex the failure
     * @return the INTERNAL_ERROR response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleUnexpected(final Exception ex) {
        LOG.error("Unexpected error", ex);
        return respond(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", null);
    }

    private static ResponseEntity<ErrorBody> respond(final ErrorCode code, final String message,
            final String field) {
        return ResponseEntity.status(code.httpStatus()).body(new ErrorBody(code.name(), message, field));
    }
}
