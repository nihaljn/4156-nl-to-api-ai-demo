package edu.columbia.subtrack.service;

import edu.columbia.subtrack.api.ApiException;
import edu.columbia.subtrack.domain.Plan;
import edu.columbia.subtrack.domain.PlanTerms;
import edu.columbia.subtrack.repository.PlanRepository;
import org.springframework.stereotype.Service;

@Service
public class PlanService {

    private final PlanRepository plans;
    private final IdempotencyService idempotency;
    private final IdGenerator ids;

    public PlanService(PlanRepository plans, IdempotencyService idempotency, IdGenerator ids) {
        this.plans = plans;
        this.idempotency = idempotency;
        this.ids = ids;
    }

    /** SPEC 5.1: stores the plan, or returns the stored one when the idempotency key was already used. */
    public Plan create(String clientKey, PlanTerms terms, String idempotencyKey) {
        return idempotency.execute(clientKey, idempotencyKey, terms, () -> store(clientKey, terms));
    }

    private Plan store(String clientKey, PlanTerms terms) {
        Plan plan = new Plan(ids.next("pln_"), terms);
        if (!plans.addIfNameFree(clientKey, plan)) {
            throw new ApiException("PLAN_NAME_TAKEN", "a plan named '" + terms.name() + "' already exists", null);
        }
        return plan;
    }
}
