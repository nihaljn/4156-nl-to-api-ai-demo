package edu.columbia.subtrack.domain;

import java.time.LocalDate;

/** A stored subscription (SPEC section 3). Status, period and next billing date are derived, never stored. */
public record Subscription(String subscriptionId, String planId, String customerId,
                           LocalDate startDate, LocalDate firstBillingOn, LocalDate endsOn) {

    public BillingCalendar calendar(Interval interval) {
        return new BillingCalendar(startDate, firstBillingOn, interval, endsOn);
    }
}
