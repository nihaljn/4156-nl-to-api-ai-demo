package edu.columbia.subtrack.api;

import edu.columbia.subtrack.domain.Interval;
import edu.columbia.subtrack.domain.Plan;

/** The stored plan as returned by POST /plans. */
public record PlanResponse(String planId, String name, long priceCents, String currency,
                           Interval interval, int trialDays) {

    static PlanResponse of(Plan plan) {
        return new PlanResponse(plan.planId(), plan.terms().name(), plan.terms().priceCents(),
                plan.terms().currency(), plan.terms().interval(), plan.terms().trialDays());
    }
}
