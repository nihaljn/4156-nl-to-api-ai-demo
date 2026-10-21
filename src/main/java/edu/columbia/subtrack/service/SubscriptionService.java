package edu.columbia.subtrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.columbia.subtrack.api.ApiException;
import edu.columbia.subtrack.api.ErrorCode;
import edu.columbia.subtrack.domain.BillingCalendar;
import edu.columbia.subtrack.domain.BillingCalendar.Status;
import edu.columbia.subtrack.domain.Plan;
import edu.columbia.subtrack.domain.Subscription;
import edu.columbia.subtrack.service.IdempotencyStore.StoredResponse;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Creates subscriptions (SPEC.md section 5.2). */
@Service
public class SubscriptionService {

    private static final int CREATED = 201;

    private final PlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final IdempotencyStore idempotency;
    private final IdGenerator ids;

    /**
     * Creates the service.
     *
     * @param plans where plans are read
     * @param subscriptions where subscriptions are stored
     * @param idempotency replays repeated requests
     * @param ids generates subscription IDs
     */
    public SubscriptionService(final PlanRepository plans, final SubscriptionRepository subscriptions,
            final IdempotencyStore idempotency, final IdGenerator ids) {
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.idempotency = idempotency;
        this.ids = ids;
    }

    /**
     * A validated subscribe request.
     *
     * @param planId the plan to subscribe to
     * @param customerId the client's customer
     * @param startDate the start date, already defaulted and checked against today
     */
    public record NewSubscription(String planId, String customerId, LocalDate startDate) {
    }

    /**
     * Stores a subscription (R1, R8) and returns its view for today (R4, R5, R6).
     *
     * @param clientKey the calling client
     * @param idempotencyKey the Idempotency-Key header, or null
     * @param body the raw request body, compared on repeats
     * @param request the validated request
     * @param today the current date
     * @return 201 with the SubscriptionView, or the stored response of an earlier identical request
     * @throws ApiException PLAN_NOT_FOUND, ALREADY_SUBSCRIBED or IDEMPOTENCY_MISMATCH
     */
    public StoredResponse create(final String clientKey, final String idempotencyKey, final JsonNode body,
            final NewSubscription request, final LocalDate today) {
        final Plan plan = plans.find(clientKey, request.planId()).orElseThrow(() -> new ApiException(
            ErrorCode.PLAN_NOT_FOUND, "No plan with id '" + request.planId() + "'", "planId"));
        return idempotency.execute(clientKey, idempotencyKey, body, () -> {
            final BillingCalendar calendar = new BillingCalendar(request.startDate(), plan.trialDays(),
                plan.interval(), null);
            final Subscription subscription = new Subscription(ids.newId("sub_"), plan.planId(),
                request.customerId(), request.startDate(), calendar.firstBillingOn(), null);
            final Optional<Subscription> blocking = subscriptions.addUnlessBlocked(clientKey, subscription,
                existing -> isLive(existing, plan, subscription, today));
            if (blocking.isPresent()) {
                throw new ApiException(ErrorCode.ALREADY_SUBSCRIBED, "Customer '" + request.customerId()
                    + "' already has a live subscription to this plan", null, blocking.get().subscriptionId());
            }
            return new StoredResponse(CREATED, SubscriptionView.of(subscription, plan, today));
        });
    }

    /** R8: same customer and plan, and not CANCELED today. */
    private static boolean isLive(final Subscription existing, final Plan plan, final Subscription candidate,
            final LocalDate today) {
        return existing.customerId().equals(candidate.customerId())
            && existing.planId().equals(candidate.planId())
            && new BillingCalendar(existing.startDate(), plan.trialDays(), plan.interval(), existing.endsOn())
                .status(today) != Status.CANCELED;
    }
}
