package edu.columbia.subtrack.domain;

import edu.columbia.subtrack.domain.BillingCalendar.Interval;

/**
 * A subscription plan (SPEC.md section 3).
 *
 * @param planId generated ID, {@code pln_} plus 10 characters
 * @param name trimmed name, unique per client ignoring case
 * @param priceCents price in integer cents
 * @param currency three-letter currency code
 * @param interval how often the plan bills
 * @param trialDays length of the trial in days
 */
public record Plan(String planId, String name, long priceCents, String currency, Interval interval,
        int trialDays) {
}
