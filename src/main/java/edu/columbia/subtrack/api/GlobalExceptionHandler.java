package edu.columbia.subtrack.api;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every failure into the error body of SPEC section 2, using only the codes of section 6. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<String, HttpStatus> STATUS_BY_CODE = Map.of(
            "UNAUTHENTICATED", HttpStatus.UNAUTHORIZED,
            "MALFORMED_REQUEST", HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR", HttpStatus.BAD_REQUEST,
            "PLAN_NOT_FOUND", HttpStatus.NOT_FOUND,
            "SUBSCRIPTION_NOT_FOUND", HttpStatus.NOT_FOUND,
            "NOT_FOUND", HttpStatus.NOT_FOUND,
            "PLAN_NAME_TAKEN", HttpStatus.CONFLICT,
            "ALREADY_SUBSCRIBED", HttpStatus.CONFLICT,
            "ALREADY_CANCELED", HttpStatus.CONFLICT,
            "IDEMPOTENCY_MISMATCH", HttpStatus.CONFLICT);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        HttpStatus status = STATUS_BY_CODE.getOrDefault(e.getCode(), HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(status).body(new ErrorResponse(e.getCode(), e.getMessage(), e.getField(),
                e.getExistingSubscriptionId()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("MALFORMED_REQUEST", "request body is missing or not valid JSON", null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleUnknownPath(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "no such endpoint", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        LOG.error("unexpected failure", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "unexpected error", null));
    }
}
