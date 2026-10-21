package edu.columbia.subtrack.api;

import edu.columbia.subtrack.domain.BillingCalendar;
import edu.columbia.subtrack.domain.Period;
import edu.columbia.subtrack.domain.Status;
import edu.columbia.subtrack.domain.Subscription;
import edu.columbia.subtrack.service.SubscriptionDetails;
import java.time.LocalDate;

/** The SubscriptionView of SPEC section 5, computed for one day under the rules of section 4. */
public record SubscriptionView(String subscriptionId, String planId, String customerId,
                               LocalDate startDate, LocalDate firstBillingOn, Status status,
                               LocalDate currentPeriodStart, LocalDate currentPeriodEnd,
                               LocalDate nextBillingOn, LocalDate endsOn, boolean cancelAtPeriodEnd) {

    static SubscriptionView of(SubscriptionDetails details) {
        Subscription subscription = details.subscription();
        LocalDate day = details.asOf();
        BillingCalendar calendar = subscription.calendar(details.interval());
        Period period = calendar.currentPeriod(day).orElse(null);
        return new SubscriptionView(subscription.subscriptionId(), subscription.planId(),
                subscription.customerId(), subscription.startDate(), subscription.firstBillingOn(),
                calendar.status(day),
                period == null ? null : period.start(), period == null ? null : period.end(),
                calendar.nextBillingOn(day).orElse(null), subscription.endsOn(),
                calendar.cancelAtPeriodEnd(day));
    }
}
