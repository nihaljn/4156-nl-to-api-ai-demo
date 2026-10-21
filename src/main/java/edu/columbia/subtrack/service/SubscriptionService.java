package edu.columbia.subtrack.service;

import edu.columbia.subtrack.api.ApiException;
import edu.columbia.subtrack.domain.BillingCalendar;
import edu.columbia.subtrack.domain.Plan;
import edu.columbia.subtrack.domain.Status;
import edu.columbia.subtrack.domain.Subscription;
import edu.columbia.subtrack.repository.PlanRepository;
import edu.columbia.subtrack.repository.SubscriptionRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {

    private final PlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final IdempotencyService idempotency;
    private final IdGenerator ids;

    public SubscriptionService(PlanRepository plans, SubscriptionRepository subscriptions,
                               IdempotencyService idempotency, IdGenerator ids) {
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.idempotency = idempotency;
        this.ids = ids;
    }

    /**
     * SPEC 5.2: stores the subscription, or returns the stored result when the idempotency key was already
     * used. {@code today} is the day the request arrived; it is the default start date and the day the
     * returned view is computed for.
     */
    public SubscriptionDetails subscribe(String clientKey, SubscribeCommand command, String idempotencyKey,
                                         LocalDate today) {
        return idempotency.execute(clientKey, idempotencyKey, command, () -> store(clientKey, command, today));
    }

    private SubscriptionDetails store(String clientKey, SubscribeCommand command, LocalDate today) {
        Plan plan = plans.findById(clientKey, command.planId())
                .orElseThrow(() -> new ApiException("PLAN_NOT_FOUND", "no plan " + command.planId(), null));
        LocalDate startDate = command.startDate() == null ? today : command.startDate();
        var subscription = new Subscription(ids.next("sub_"), plan.planId(), command.customerId(), startDate,
                BillingCalendar.firstBillingOn(startDate, plan.terms().trialDays()), null);
        subscriptions.addUnlessBlocked(clientKey, subscription, existing -> isLive(existing, plan, command, today))
                .ifPresent(existing -> {
                    throw new ApiException("ALREADY_SUBSCRIBED",
                            "customer " + command.customerId() + " already has a live subscription to this plan",
                            null, existing.subscriptionId());
                });
        return new SubscriptionDetails(subscription, plan.terms().interval(), today);
    }

    /** R8: same customer and plan, and not CANCELED today. */
    private static boolean isLive(Subscription existing, Plan plan, SubscribeCommand command, LocalDate today) {
        return existing.planId().equals(plan.planId())
                && existing.customerId().equals(command.customerId())
                && existing.calendar(plan.terms().interval()).status(today) != Status.CANCELED;
    }
}
