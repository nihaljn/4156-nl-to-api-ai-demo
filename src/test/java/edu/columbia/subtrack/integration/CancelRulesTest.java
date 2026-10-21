package edu.columbia.subtrack.integration;

import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.support.ApiTestBase;
import edu.columbia.subtrack.support.SubTrackApi.Response;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** R7 (cancel) and R8 (one live subscription), end to end. */
class CancelRulesTest extends ApiTestBase {

    enum State { TRIALING, ACTIVE, CANCEL_SCHEDULED, CANCELED }

    /**
     * Each state is reached on 2026-03-05 with plan M7, then the clock moves to 2026-03-06 so a
     * request that "changes nothing" is told apart from one that re-stamps endsOn with today.
     * TRIALING starts 03-01 (first billing 03-08); the others start 01-24 (period 02-28 to 03-31).
     */
    private String subscriptionIn(State state) {
        String planId = m7();
        String subscriptionId = api.subscribeOk(client, planId, "cus_42",
                state == State.TRIALING ? "2026-03-01" : "2026-01-24");
        switch (state) {
            case CANCEL_SCHEDULED -> api.cancel(client, subscriptionId, "AT_PERIOD_END").expectStatus(200);
            case CANCELED -> api.cancel(client, subscriptionId, "IMMEDIATE").expectStatus(200);
            default -> { }
        }
        clock.advance(Duration.ofDays(1));
        return subscriptionId;
    }

    static Stream<Arguments> cancelStateTable() {
        return Stream.of(
                // state,              mode,           http, endsOn,       status,     cancelAtPeriodEnd
                Arguments.of(State.TRIALING, "AT_PERIOD_END", 200, "2026-03-08", "TRIALING", true),
                Arguments.of(State.TRIALING, "IMMEDIATE", 200, "2026-03-06", "CANCELED", false),
                Arguments.of(State.ACTIVE, "AT_PERIOD_END", 200, "2026-03-31", "ACTIVE", true),
                Arguments.of(State.ACTIVE, "IMMEDIATE", 200, "2026-03-06", "CANCELED", false),
                Arguments.of(State.CANCEL_SCHEDULED, "AT_PERIOD_END", 200, "2026-03-31", "ACTIVE", true),
                Arguments.of(State.CANCEL_SCHEDULED, "IMMEDIATE", 200, "2026-03-06", "CANCELED", false),
                Arguments.of(State.CANCELED, "AT_PERIOD_END", 409, "2026-03-05", "CANCELED", false),
                Arguments.of(State.CANCELED, "IMMEDIATE", 200, "2026-03-05", "CANCELED", false));
    }

    @ParameterizedTest(name = "R7 {0} x {1}")
    @MethodSource("cancelStateTable")
    void R7_cancelStateTable(State state, String mode, int httpStatus, String endsOn,
                             String status, boolean cancelAtPeriodEnd) {
        String subscriptionId = subscriptionIn(state);

        Response response = api.cancel(client, subscriptionId, mode);

        if (httpStatus == 409) {
            response.expectError(409, "ALREADY_CANCELED");
        } else {
            response.expectStatus(200);
            assertCancelView(response, endsOn, status, cancelAtPeriodEnd);
        }
        assertCancelView(api.view(client, subscriptionId, null).expectStatus(200),
                endsOn, status, cancelAtPeriodEnd);
    }

    private static void assertCancelView(Response view, String endsOn, String status,
                                         boolean cancelAtPeriodEnd) {
        assertThat(view.text("endsOn")).isEqualTo(endsOn);
        assertThat(view.text("status")).isEqualTo(status);
        assertThat(view.flag("cancelAtPeriodEnd")).isEqualTo(cancelAtPeriodEnd);
        if (status.equals("CANCELED")) {
            assertThat(view.isNull("currentPeriodStart")).isTrue();
            assertThat(view.isNull("currentPeriodEnd")).isTrue();
            assertThat(view.isNull("nextBillingOn")).isTrue();
        }
    }

    @Test
    void R7_atPeriodEndOnABillingDayEndsAtTheNextBillingDate() {
        // start 02-05 with no trial: today (03-05) is billing(1), so the period is [03-05, 04-05)
        String subscriptionId = api.subscribeOk(client, m0(), "cus_42", "2026-02-05");

        Response canceled = api.cancel(client, subscriptionId, "AT_PERIOD_END").expectStatus(200);

        assertThat(canceled.text("endsOn")).isEqualTo("2026-04-05");
    }

    @Test
    void R7_atPeriodEndUsesTheMonthEndAnchor() {
        // start 01-31: billing dates 02-28, 03-31; today (03-05) is inside [02-28, 03-31)
        String subscriptionId = api.subscribeOk(client, m0(), "cus_42", "2026-01-31");

        Response canceled = api.cancel(client, subscriptionId, "AT_PERIOD_END").expectStatus(200);

        assertThat(canceled.text("endsOn")).isEqualTo("2026-03-31");
    }

    @Test
    void R7_atPeriodEndOnAYearlyPlanEndsAfterTheYear() {
        String subscriptionId = api.subscribeOk(client, y0(), "cus_42", "2025-06-15");

        Response canceled = api.cancel(client, subscriptionId, "AT_PERIOD_END").expectStatus(200);

        assertThat(canceled.text("endsOn")).isEqualTo("2026-06-15");
    }

    @Test
    void R7_scheduledCancelBecomesCanceledOnceTimePassesEndsOn() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");
        api.cancel(client, subscriptionId, "AT_PERIOD_END").expectStatus(200);

        clock.advance(Duration.ofDays(26));

        Response view = api.view(client, subscriptionId, null).expectStatus(200);
        assertThat(view.text("status")).isEqualTo("CANCELED");
        api.cancel(client, subscriptionId, "AT_PERIOD_END").expectError(409, "ALREADY_CANCELED");
    }

    // ---- R8 ----------------------------------------------------------------------------------

    @Test
    void R8_sameCustomerAndPlanIsRejectedWhileTrialing() {
        String planId = m7();
        String first = api.subscribeOk(client, planId, "cus_42", "2026-03-01");

        Response rejected = api.subscribe(client, planId, "cus_42", null)
                .expectError(409, "ALREADY_SUBSCRIBED");
        assertThat(rejected.text("existingSubscriptionId")).isEqualTo(first);
    }

    @Test
    void R8_aScheduledCancelStillBlocksUntilItTakesEffect() {
        String planId = m7();
        String first = api.subscribeOk(client, planId, "cus_42", "2026-01-24");
        api.cancel(client, first, "AT_PERIOD_END").expectStatus(200);

        api.subscribe(client, planId, "cus_42", null).expectError(409, "ALREADY_SUBSCRIBED");

        clock.advance(Duration.ofDays(26));
        api.subscribe(client, planId, "cus_42", null).expectStatus(201);
    }

    @Test
    void R8_aDifferentCustomerMaySubscribeToTheSamePlan() {
        String planId = m7();
        api.subscribeOk(client, planId, "cus_42", null);

        api.subscribe(client, planId, "cus_43", null).expectStatus(201);
    }

    @Test
    void R8_theSameCustomerMaySubscribeToADifferentPlan() {
        api.subscribeOk(client, m7(), "cus_42", null);
        String otherPlan = api.createPlan(client, "Y0", "YEAR", 9999, 0);

        api.subscribe(client, otherPlan, "cus_42", null).expectStatus(201);
    }

    @Test
    void R8_aCanceledSubscriptionDoesNotBlockAndTheNewOneStartsFresh() {
        String planId = m7();
        String first = api.subscribeOk(client, planId, "cus_42", "2026-01-24");
        api.cancel(client, first, "IMMEDIATE").expectStatus(200);

        Response second = api.subscribe(client, planId, "cus_42", null).expectStatus(201);

        assertThat(second.text("subscriptionId")).isNotEqualTo(first);
        assertThat(second.text("status")).isEqualTo("TRIALING");
    }

    @Test
    void R8_conflictIsReportedAfterValidationAndNotFound() {
        String planId = m7();
        api.subscribeOk(client, planId, "cus_42", null);

        api.subscribe(client, planId, "bad customer!", null).expectValidationError("customerId");
        api.subscribe(client, "pln_0000000000", "cus_42", null).expectError(404, "PLAN_NOT_FOUND");
    }
}
