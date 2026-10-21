package edu.columbia.subtrack.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        final JsonNode body = RequestBodies.parseObject(mapper, rawBody);
        RequestBodies.checkIdempotencyKey(idempotencyKey);
        final StoredResponse response = planService.create(clientKey, idempotencyKey, body);
        return ResponseEntity.status(response.status()).body(response.body());
    }
}
