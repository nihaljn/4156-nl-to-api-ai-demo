package edu.columbia.subtrack.domain;

/** The validated, client-supplied part of a plan (SPEC section 3). */
public record PlanTerms(String name, long priceCents, String currency, Interval interval, int trialDays) {
}
