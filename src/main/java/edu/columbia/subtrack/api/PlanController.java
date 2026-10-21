package edu.columbia.subtrack.api;

import edu.columbia.subtrack.service.PlanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlanController {

    private final PlanService plans;

    public PlanController(PlanService plans) {
        this.plans = plans;
    }

    /** SPEC 5.1. */
    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    PlanResponse create(@RequestAttribute(ClientKeyFilter.CLIENT_KEY) String clientKey,
                        @RequestBody PlanRequest request,
                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var terms = request.validated();
        IdempotencyKeys.check(idempotencyKey);
        return PlanResponse.of(plans.create(clientKey, terms, idempotencyKey));
    }
}
