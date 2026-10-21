package edu.columbia.subtrack.integration;

import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.support.ApiTestBase;
import edu.columbia.subtrack.support.SubTrackApi.Response;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * SPEC section 7, AT-1 to AT-10, AT-14 and AT-15, end to end. AT-11 to AT-13 live in
 * IdempotencyTest, ConcurrencyTest and IsolationTest; AT-16 in ErrorHandlingWebMvcTest.
 */
class AcceptanceTest extends ApiTestBase {

    @Test
    void AT1_R2_billingDatesKeepMonthEndAnchorFromJan31() {
        api.subscribeOk(client, m0(), "cus_42", "2026-01-31");

        Response charges = api.billingDates(client, "2026-02-01", "2026-04-30", null).expectStatus(200);

        assertThat(charges.chargeDates()).containsExactly("2026-02-28", "2026-03-31", "2026-04-30");
    }

    @Test
    void AT2_R2_billingDatesKeepMonthEndAnchorFromJan30() {
        api.subscribeOk(client, m0(), "cus_42", "2026-01-30");

        Response charges = api.billingDates(client, "2026-02-01", "2026-03-31", null).expectStatus(200);

        assertThat(charges.chargeDates()).containsExactly("2026-02-28", "2026-03-30");
    }

    @Test
    void AT3_R2R4_yearlyPeriodFollowsTheLeapDayAnchor() {
        clock.setDate(LocalDate.parse("2032-03-01"));
        String subscriptionId = api.subscribeOk(client, y0(), "cus_42", "2028-02-29");

        Response in2029 = api.view(client, subscriptionId, "2029-03-01").expectStatus(200);
        Response in2032 = api.view(client, subscriptionId, "2032-03-01").expectStatus(200);

        assertThat(in2029.text("currentPeriodStart")).isEqualTo("2029-02-28");
        assertThat(in2032.text("currentPeriodStart")).isEqualTo("2032-02-29");
    }

    @Test
    void AT4_R1R5R6_trialEndsOnFirstBillingDate() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        Response duringTrial = api.view(client, subscriptionId, "2026-01-30").expectStatus(200);
        Response onFirstBilling = api.view(client, subscriptionId, "2026-01-31").expectStatus(200);

        assertThat(duringTrial.text("status")).isEqualTo("TRIALING");
        assertThat(onFirstBilling.text("status")).isEqualTo("ACTIVE");
        assertThat(onFirstBilling.text("nextBillingOn")).isEqualTo("2026-02-28");
    }

    @Test
    void AT5_R3R6R7_cancelAtPeriodEndSchedulesTheEndAndStopsBilling() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        Response canceled = api.cancel(client, subscriptionId, "AT_PERIOD_END").expectStatus(200);

        assertThat(canceled.text("endsOn")).isEqualTo("2026-03-31");
        assertThat(canceled.flag("cancelAtPeriodEnd")).isTrue();
        assertThat(api.view(client, subscriptionId, "2026-03-31").text("status")).isEqualTo("CANCELED");
        assertThat(api.billingDates(client, "2026-03-01", "2026-03-31", null)
                .expectStatus(200).chargeDates()).isEmpty();
    }

    @Test
    void AT6_R3R7_cancelAtPeriodEndDuringTheTrialEndsAtFirstBilling() {
        clock.setDate(LocalDate.parse("2026-01-26"));
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        Response canceled = api.cancel(client, subscriptionId, "AT_PERIOD_END").expectStatus(200);

        assertThat(canceled.text("endsOn")).isEqualTo("2026-01-31");
        assertThat(api.billingDates(client, "2026-01-01", "2026-03-31", null)
                .expectStatus(200).chargeDates()).isEmpty();
    }

    @Test
    void AT7_R7_immediateCancelTwiceIsIdempotent() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        Response first = api.cancel(client, subscriptionId, "IMMEDIATE").expectStatus(200);
        Response second = api.cancel(client, subscriptionId, "IMMEDIATE").expectStatus(200);

        for (Response response : new Response[] {first, second}) {
            assertThat(response.text("status")).isEqualTo("CANCELED");
            assertThat(response.text("endsOn")).isEqualTo("2026-03-05");
        }
    }

    @Test
    void AT8_R7_atPeriodEndOnCanceledSubscriptionReturns409() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");
        api.cancel(client, subscriptionId, "IMMEDIATE").expectStatus(200);

        api.cancel(client, subscriptionId, "AT_PERIOD_END").expectError(409, "ALREADY_CANCELED");
    }

    @Test
    void AT9_R7_immediateCancelOverridesAScheduledCancel() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");
        api.cancel(client, subscriptionId, "AT_PERIOD_END").expectStatus(200);
        clock.setDate(LocalDate.parse("2026-03-10"));

        Response canceled = api.cancel(client, subscriptionId, "IMMEDIATE").expectStatus(200);

        assertThat(canceled.text("endsOn")).isEqualTo("2026-03-10");
        assertThat(canceled.text("status")).isEqualTo("CANCELED");
    }

    @Test
    void AT10_R8_secondLiveSubscriptionForTheSameCustomerAndPlanIsRejected() {
        String planId = m7();
        String first = api.subscribeOk(client, planId, "cus_42", null);

        Response rejected = api.subscribe(client, planId, "cus_42", null)
                .expectError(409, "ALREADY_SUBSCRIBED");
        assertThat(rejected.text("existingSubscriptionId")).isEqualTo(first);

        api.cancel(client, first, "IMMEDIATE").expectStatus(200);
        api.subscribe(client, planId, "cus_42", null).expectStatus(201);
    }

    @Test
    void AT14_S5_5_billingDatesWindowIsCappedAt92Days() {
        api.billingDates(client, "2026-03-01", "2026-06-01", null).expectValidationError("to");

        Response ok = api.billingDates(client, "2026-03-01", "2026-05-31", null).expectStatus(200);
        assertThat(ok.body().get("charges").isArray()).isTrue();
    }

    @Test
    void AT15_S5_2_startDateAfterTodayIsRejected() {
        String planId = m7();

        api.subscribe(client, planId, "cus_42", "2026-03-06").expectValidationError("startDate");
    }
}
