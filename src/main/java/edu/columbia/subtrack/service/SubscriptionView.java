package edu.columbia.subtrack.service;

import edu.columbia.subtrack.domain.BillingCalendar;
import edu.columbia.subtrack.domain.BillingCalendar.Period;
import edu.columbia.subtrack.domain.BillingCalendar.Status;
import edu.columbia.subtrack.domain.Plan;
import edu.columbia.subtrack.domain.Subscription;
import java.time.LocalDate;

/**
 * A subscription as returned by the API, computed for one day (SPEC.md section 5).
 *
 * @param subscriptionId the subscription
 * @param planId the plan
 * @param customerId the customer
 * @param startDate the start date
 * @param firstBillingOn the first billing date
 * @param status the status on the day
 * @param currentPeriodStart the current period's first day, null when canceled
 * @param currentPeriodEnd the current period's exclusive end, null when canceled
 * @param nextBillingOn the next counting billing date, or null
 * @param endsOn the end date, or null
 * @param cancelAtPeriodEnd whether a cancel is scheduled but not yet in effect
 */
public record SubscriptionView(String subscriptionId, String planId, String customerId, LocalDate startDate,
        LocalDate firstBillingOn, Status status, LocalDate currentPeriodStart, LocalDate currentPeriodEnd,
        LocalDate nextBillingOn, LocalDate endsOn, boolean cancelAtPeriodEnd) {

    /**
     * Computes the view of a subscription for a day.
     *
     * @param subscription the stored subscription
     * @param plan its plan
     * @param day the day to evaluate (R4, R5, R6)
     * @return the view
     */
    public static SubscriptionView of(final Subscription subscription, final Plan plan, final LocalDate day) {
        final BillingCalendar calendar = new BillingCalendar(subscription.startDate(), plan.trialDays(),
            plan.interval(), subscription.endsOn());
        final Period period = calendar.currentPeriod(day);
        return new SubscriptionView(subscription.subscriptionId(), subscription.planId(),
            subscription.customerId(), subscription.startDate(), calendar.firstBillingOn(),
            calendar.status(day), period == null ? null : period.start(), period == null ? null : period.end(),
            calendar.nextBillingOn(day), subscription.endsOn(), calendar.cancelAtPeriodEnd(day));
    }
}
