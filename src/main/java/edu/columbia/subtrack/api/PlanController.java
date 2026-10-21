package edu.columbia.subtrack.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import edu.columbia.subtrack.service.IdempotencyStore.StoredResponse;
import edu.columbia.subtrack.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** POST /plans (SPEC.md section 5.1). */
@RestController
public class PlanController {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final PlanService planService;
    private final ObjectMapper mapper;

    /**
     * Creates the controller.
     *
     * @param planService creates plans
     * @param mapper parses the request body
     */
    public PlanController(final PlanService planService, final ObjectMapper mapper) {
        this.planService = planService;
        this.mapper = mapper;
    }

    /**
     * Creates a plan.
     *
     * @param clientKey the calling client
     * @param idempotencyKey optional key that makes repeats safe
     * @param rawBody the request body
     * @return 201 with the plan
     */
    @PostMapping("/plans")
    public ResponseEntity<Object> create(@RequestHeader("X-Client-Key") final String clientKey,
            @RequestHeader(value = "Idempotency-Key", required = false) final String idempotencyKey,
            @RequestBody(required = false) final String rawBody) {
        final JsonNode body = parse(rawBody);
        if (idempotencyKey != null && (idempotencyKey.isEmpty()
                || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "Idempotency-Key must be between 1 and 100 characters", "Idempotency-Key");
        }
        final StoredResponse response = planService.create(clientKey, idempotencyKey, body);
        return ResponseEntity.status(response.status()).body(response.body());
    }

    private JsonNode parse(final String rawBody) {
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
}
