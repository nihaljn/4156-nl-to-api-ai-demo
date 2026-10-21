package edu.columbia.subtrack.domain;

import java.time.LocalDate;

/**
 * A subscription (SPEC.md section 3). Status, period and next billing date are derived, never stored.
 *
 * @param subscriptionId generated ID, {@code sub_} plus 10 characters
 * @param planId the client's plan
 * @param customerId the client's customer
 * @param startDate the day the subscription starts
 * @param firstBillingOn startDate plus the plan's trial days (R1)
 * @param endsOn the day the subscription ends, or null
 */
public record Subscription(String subscriptionId, String planId, String customerId, LocalDate startDate,
        LocalDate firstBillingOn, LocalDate endsOn) {
}
