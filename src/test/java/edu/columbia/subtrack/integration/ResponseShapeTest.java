package edu.columbia.subtrack.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.columbia.subtrack.support.ApiTestBase;
import edu.columbia.subtrack.support.SubTrackApi.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

/** What each endpoint returns (SPEC section 5): SubscriptionView, charges list, read-only GET. */
class ResponseShapeTest extends ApiTestBase {

    private static final List<String> VIEW_FIELDS = List.of("subscriptionId", "planId", "customerId",
            "startDate", "firstBillingOn", "status", "currentPeriodStart", "currentPeriodEnd",
            "nextBillingOn", "endsOn", "cancelAtPeriodEnd");

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @Test
    void S5_2_subscribeReturnsTheSubscriptionViewForToday() {
        String planId = m7();

        Response view = api.subscribe(client, planId, "cus_42", "2026-01-24").expectStatus(201);

        assertThat(fieldNames(view.body())).containsExactlyInAnyOrderElementsOf(VIEW_FIELDS);
        assertThat(view.text("subscriptionId")).matches("sub_[a-z0-9]{10}");
        assertThat(view.text("planId")).isEqualTo(planId);
        assertThat(view.text("customerId")).isEqualTo("cus_42");
        assertThat(view.text("startDate")).isEqualTo("2026-01-24");
        assertThat(view.text("firstBillingOn")).isEqualTo("2026-01-31");
        assertThat(view.text("status")).isEqualTo("ACTIVE");
        assertThat(view.text("currentPeriodStart")).isEqualTo("2026-02-28");
        assertThat(view.text("currentPeriodEnd")).isEqualTo("2026-03-31");
        assertThat(view.text("nextBillingOn")).isEqualTo("2026-03-31");
        assertThat(view.isNull("endsOn")).isTrue();
        assertThat(view.flag("cancelAtPeriodEnd")).isFalse();
    }

    @Test
    void R1_firstBillingOnIsStartDatePlusTheTrial() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-02-20");

        Response view = api.view(client, subscriptionId, null).expectStatus(200);

        assertThat(view.text("firstBillingOn")).isEqualTo("2026-02-27");
        assertThat(view.text("status")).isEqualTo("ACTIVE");
    }

    @Test
    void S5_3_getReturnsTheViewForAsOfAndDefaultsToToday() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        Response today = api.view(client, subscriptionId, null).expectStatus(200);
        Response explicitToday = api.view(client, subscriptionId, "2026-03-05").expectStatus(200);
        Response trial = api.view(client, subscriptionId, "2026-01-24").expectStatus(200);

        assertThat(today.raw()).isEqualTo(explicitToday.raw());
        assertThat(trial.text("status")).isEqualTo("TRIALING");
        assertThat(trial.text("currentPeriodStart")).isEqualTo("2026-01-24");
        assertThat(trial.text("currentPeriodEnd")).isEqualTo("2026-01-31");
    }

    @Test
    void S5_3_asOfMayBeInTheFuture() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        Response later = api.view(client, subscriptionId, "2027-06-15").expectStatus(200);

        assertThat(later.text("status")).isEqualTo("ACTIVE");
        assertThat(later.text("currentPeriodStart")).isEqualTo("2027-05-31");
        assertThat(later.text("currentPeriodEnd")).isEqualTo("2027-06-30");
    }

    @Test
    void S5_3_getIsReadOnly() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");
        String before = api.view(client, subscriptionId, null).raw();

        api.view(client, subscriptionId, "2030-01-01").expectStatus(200);

        assertThat(api.view(client, subscriptionId, null).raw()).isEqualTo(before);
    }

    @Test
    void S5_4_cancelReturnsTheViewForToday() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        Response canceled = api.cancel(client, subscriptionId, "IMMEDIATE").expectStatus(200);

        assertThat(fieldNames(canceled.body())).containsExactlyInAnyOrderElementsOf(VIEW_FIELDS);
        assertThat(canceled.isNull("currentPeriodStart")).isTrue();
        assertThat(canceled.isNull("currentPeriodEnd")).isTrue();
        assertThat(canceled.isNull("nextBillingOn")).isTrue();
    }

    @Test
    void S5_5_chargesCarryTheScheduleFieldsForTheWorkedExample() {
        String planId = m7();
        String subscriptionId = api.subscribeOk(client, planId, "cus_42", "2026-01-24");

        Response response = api.billingDates(client, "2026-02-01", "2026-03-31", null).expectStatus(200);

        assertThat(response.text("from")).isEqualTo("2026-02-01");
        assertThat(response.text("to")).isEqualTo("2026-03-31");
        JsonNode charges = response.body().get("charges");
        assertThat(charges).hasSize(2);
        for (JsonNode charge : charges) {
            assertThat(fieldNames(charge)).containsExactlyInAnyOrder("date", "subscriptionId",
                    "customerId", "planId", "amountCents", "currency");
            assertThat(charge.get("subscriptionId").asText()).isEqualTo(subscriptionId);
            assertThat(charge.get("customerId").asText()).isEqualTo("cus_42");
            assertThat(charge.get("planId").asText()).isEqualTo(planId);
            assertThat(charge.get("amountCents").isIntegralNumber()).isTrue();
            assertThat(charge.get("amountCents").asLong()).isEqualTo(999);
            assertThat(charge.get("currency").asText()).isEqualTo("USD");
        }
        assertThat(response.chargeDates()).containsExactly("2026-02-28", "2026-03-31");
    }

    @Test
    void S5_5_chargeAmountComesFromThePlan() {
        String yearly = api.createPlan(client, "Annual EUR", "YEAR", 12_000, "EUR", 0);
        api.subscribeOk(client, yearly, "cus_42", "2025-03-10");

        Response response = api.billingDates(client, "2026-03-01", "2026-03-31", null).expectStatus(200);

        JsonNode charge = response.body().get("charges").get(0);
        assertThat(charge.get("date").asText()).isEqualTo("2026-03-10");
        assertThat(charge.get("amountCents").asLong()).isEqualTo(12_000);
        assertThat(charge.get("currency").asText()).isEqualTo("EUR");
    }

    @Test
    void S5_5_chargesAreSortedByDateThenSubscriptionId() {
        String planId = m0();
        for (String customer : List.of("cus_1", "cus_2", "cus_3")) {
            api.subscribeOk(client, planId, customer, "2026-01-31");
        }
        String monthEndPlan = api.createPlan(client, "M0-b", "MONTH", 500, 0);
        api.subscribeOk(client, monthEndPlan, "cus_1", "2026-01-30");

        Response response = api.billingDates(client, "2026-02-01", "2026-04-30", null).expectStatus(200);

        List<String> keys = new ArrayList<>();
        response.body().get("charges").forEach(charge ->
                keys.add(charge.get("date").asText() + "|" + charge.get("subscriptionId").asText()));
        assertThat(keys).hasSize(12);
        assertThat(keys).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void S5_5_customerIdFiltersTheListing() {
        String planId = m0();
        api.subscribeOk(client, planId, "cus_1", "2026-01-31");
        api.subscribeOk(client, planId, "cus_2", "2026-01-31");

        Response filtered = api.billingDates(client, "2026-02-01", "2026-03-31", "cus_2").expectStatus(200);
        Response everyone = api.billingDates(client, "2026-02-01", "2026-03-31", null).expectStatus(200);
        Response nobody = api.billingDates(client, "2026-02-01", "2026-03-31", "cus_9").expectStatus(200);

        assertThat(filtered.body().get("charges")).hasSize(2);
        filtered.body().get("charges").forEach(charge ->
                assertThat(charge.get("customerId").asText()).isEqualTo("cus_2"));
        assertThat(everyone.body().get("charges")).hasSize(4);
        assertThat(nobody.body().get("charges")).isEmpty();
    }

    @Test
    void S5_5_datesBeforeTodayAreIncluded() {
        api.subscribeOk(client, m0(), "cus_42", "2026-01-31");

        Response past = api.billingDates(client, "2026-02-01", "2026-02-28", null).expectStatus(200);

        assertThat(past.chargeDates()).containsExactly("2026-02-28");
    }

    @Test
    void S5_5_aWindowWithNoChargesReturnsAnEmptyListNotAnError() {
        Response empty = api.billingDates(client, "2026-03-01", "2026-03-31", null).expectStatus(200);

        assertThat(empty.body().has("charges")).isTrue();
        assertThat(empty.body().get("charges").isArray()).isTrue();
        assertThat(empty.body().get("charges")).isEmpty();
    }

    @Test
    void S5_5_aCanceledSubscriptionStopsAppearingAtEndsOn() {
        String subscriptionId = api.subscribeOk(client, m0(), "cus_42", "2026-01-31");
        api.cancel(client, subscriptionId, "AT_PERIOD_END").expectStatus(200);

        Response response = api.billingDates(client, "2026-02-01", "2026-04-30", null).expectStatus(200);

        assertThat(response.chargeDates()).containsExactly("2026-02-28");
    }
}
