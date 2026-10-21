package edu.columbia.subtrack.api;

/** Validation of the optional Idempotency-Key header (SPEC section 2). */
final class IdempotencyKeys {

    private static final int MAX_LENGTH = 100;

    private IdempotencyKeys() {
    }

    static void check(String key) {
        if (key != null && (key.isEmpty() || key.length() > MAX_LENGTH)) {
            throw new ApiException("VALIDATION_ERROR", "Idempotency-Key must be 1 to 100 characters", null);
        }
    }
}
