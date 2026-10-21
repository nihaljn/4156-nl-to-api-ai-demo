package edu.columbia.subtrack.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

/** Request checks shared by the POST endpoints (SPEC.md section 2). */
final class RequestBodies {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private RequestBodies() {
    }

    /**
     * Parses a body that must be a JSON object.
     *
     * @param mapper the JSON parser
     * @param rawBody the body text, possibly null
     * @return the parsed object
     * @throws ApiException MALFORMED_REQUEST if the body is missing, invalid or not an object
     */
    static JsonNode parseObject(final ObjectMapper mapper, final String rawBody) {
        JsonNode node = MissingNode.getInstance();
        if (rawBody != null && !rawBody.isBlank()) {
            try {
                node = mapper.readerFor(JsonNode.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(rawBody);
            } catch (JsonProcessingException e) {
                node = MissingNode.getInstance();
            }
        }
        if (!node.isObject()) {
            throw new ApiException(ErrorCode.MALFORMED_REQUEST, "Request body must be a valid JSON object", null);
        }
        return node;
    }

    /**
     * Checks the optional Idempotency-Key header.
     *
     * @param idempotencyKey the header value, or null
     * @throws ApiException VALIDATION_ERROR if it is empty or longer than 100 characters
     */
    static void checkIdempotencyKey(final String idempotencyKey) {
        if (idempotencyKey != null && (idempotencyKey.isEmpty()
                || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "Idempotency-Key must be between 1 and 100 characters", "Idempotency-Key");
        }
    }
}
