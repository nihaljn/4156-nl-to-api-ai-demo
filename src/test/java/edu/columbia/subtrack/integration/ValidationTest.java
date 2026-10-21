package edu.columbia.subtrack.integration;

import static edu.columbia.subtrack.support.SubTrackApi.json;
import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.support.ApiTestBase;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Field constraints (SPEC section 3), field order, and the check order of section 5. */
class ValidationTest extends ApiTestBase {

    private static Map<String, Object> plan(Object... overrides) {
        Map<String, Object> body = json("name", "Plan-" + UUID.randomUUID(), "priceCents", 999,
                "currency", "USD", "interval", "MONTH", "trialDays", 7);
        for (int i = 0; i < overrides.length; i += 2) {
            if (overrides[i + 1] == null) {
                body.remove((String) overrides[i]);
            } else {
                body.put((String) overrides[i], overrides[i + 1]);
            }
        }
        return body;
    }

    // ---- POST /plans -------------------------------------------------------------------------

    static Stream<Arguments> invalidPlans() {
        return Stream.of(
                Arguments.of("name missing", plan("name", null), "name"),
                Arguments.of("name empty", plan("name", ""), "name"),
                Arguments.of("name blank", plan("name", "   "), "name"),
                Arguments.of("name 101 chars", plan("name", "n".repeat(101)), "name"),
                Arguments.of("priceCents missing", plan("priceCents", null), "priceCents"),
                Arguments.of("priceCents 0", plan("priceCents", 0), "priceCents"),
                Arguments.of("priceCents negative", plan("priceCents", -5), "priceCents"),
                Arguments.of("priceCents 10,000,001", plan("priceCents", 10_000_001), "priceCents"),
                Arguments.of("currency missing", plan("currency", null), "currency"),
                Arguments.of("currency empty", plan("currency", ""), "currency"),
                Arguments.of("currency lowercase", plan("currency", "usd"), "currency"),
                Arguments.of("currency too short", plan("currency", "US"), "currency"),
                Arguments.of("currency too long", plan("currency", "USDX"), "currency"),
                Arguments.of("currency not a real code", plan("currency", "ZZZ"), "currency"),
                Arguments.of("interval missing", plan("interval", null), "interval"),
                Arguments.of("interval unknown", plan("interval", "WEEK"), "interval"),
                Arguments.of("trialDays missing", plan("trialDays", null), "trialDays"),
                Arguments.of("trialDays -1", plan("trialDays", -1), "trialDays"),
                Arguments.of("trialDays 31", plan("trialDays", 31), "trialDays"));
    }

    @ParameterizedTest(name = "POST /plans: {0}")
    @MethodSource("invalidPlans")
    void S3_invalidPlanIsRejectedNamingTheField(String label, Map<String, Object> body, String field) {
        api.postPlan(client, body, null).expectValidationError(field);
    }

    static Stream<Arguments> validPlanBoundaries() {
        return Stream.of(
                Arguments.of("name 1 char", plan("name", "n")),
                Arguments.of("name 100 chars", plan("name", "n".repeat(100))),
                Arguments.of("name 100 chars padded with spaces", plan("name", "  " + "m".repeat(100) + "  ")),
                Arguments.of("priceCents 1", plan("priceCents", 1)),
                Arguments.of("priceCents 10,000,000", plan("priceCents", 10_000_000)),
                Arguments.of("currency EUR", plan("currency", "EUR")),
                Arguments.of("interval YEAR", plan("interval", "YEAR")),
                Arguments.of("trialDays 0", plan("trialDays", 0)),
                Arguments.of("trialDays 30", plan("trialDays", 30)));
    }

    @ParameterizedTest(name = "POST /plans accepts {0}")
    @MethodSource("validPlanBoundaries")
    void S3_planAtTheLimitIsAccepted(String label, Map<String, Object> body) {
        api.postPlan(client, body, null).expectStatus(201);
    }

    @Test
    void S5_1_createdPlanEchoesItsFieldsWithAGeneratedId() {
        var created = api.postPlan(client, plan("name", "Pro monthly", "priceCents", 999, "currency", "USD",
                "interval", "MONTH", "trialDays", 7), null).expectStatus(201);

        assertThat(created.text("planId")).matches("pln_[a-z0-9]{10}");
        assertThat(created.text("name")).isEqualTo("Pro monthly");
        assertThat(created.body().get("priceCents").asLong()).isEqualTo(999);
        assertThat(created.text("currency")).isEqualTo("USD");
        assertThat(created.text("interval")).isEqualTo("MONTH");
        assertThat(created.body().get("trialDays").asInt()).isEqualTo(7);
    }

    @Test
    void S5_1_planNameIsUniquePerClientIgnoringCase() {
        api.postPlan(client, plan("name", "Pro Monthly"), null).expectStatus(201);

        api.postPlan(client, plan("name", "pro monthly"), null).expectError(409, "PLAN_NAME_TAKEN");
        api.postPlan(client, plan("name", "PRO MONTHLY"), null).expectError(409, "PLAN_NAME_TAKEN");
    }

    @Test
    void S5_fieldsAreCheckedInTheOrderListed() {
        Map<String, Object> allBad = plan("name", "", "priceCents", 0, "currency", "usd",
                "interval", "WEEK", "trialDays", 99);

        api.postPlan(client, allBad, null).expectValidationError("name");
        allBad.put("name", "ok");
        api.postPlan(client, allBad, null).expectValidationError("priceCents");
        allBad.put("priceCents", 999);
        api.postPlan(client, allBad, null).expectValidationError("currency");
        allBad.put("currency", "USD");
        api.postPlan(client, allBad, null).expectValidationError("interval");
        allBad.put("interval", "MONTH");
        api.postPlan(client, allBad, null).expectValidationError("trialDays");
    }

    @Test
    void S5_validationComesBeforeAPlanNameConflict() {
        api.postPlan(client, plan("name", "Pro"), null).expectStatus(201);

        api.postPlan(client, plan("name", "Pro", "trialDays", 99), null).expectValidationError("trialDays");
    }

    // ---- POST /subscriptions -----------------------------------------------------------------

    private static Map<String, Object> subscription(String planId, Object customerId, Object startDate) {
        return json("planId", planId, "customerId", customerId, "startDate", startDate);
    }

    static Stream<Arguments> invalidCustomerIds() {
        return Stream.of(
                Arguments.of("missing", null),
                Arguments.of("empty", ""),
                Arguments.of("65 chars", "c".repeat(65)),
                Arguments.of("space", "cus 42"),
                Arguments.of("dot", "cus.42"),
                Arguments.of("slash", "cus/42"),
                Arguments.of("non-ASCII letter", "cus_é"));
    }

    @ParameterizedTest(name = "customerId {0}")
    @MethodSource("invalidCustomerIds")
    void S3_invalidCustomerIdIsRejectedNamingTheField(String label, String customerId) {
        String planId = m7();

        api.postSubscription(client, subscription(planId, customerId, null), null)
                .expectValidationError("customerId");
    }

    @ParameterizedTest(name = "customerId [{0}] accepted")
    @MethodSource("validCustomerIds")
    void S3_customerIdAtTheLimitIsAccepted(String customerId) {
        String planId = m7();

        api.postSubscription(client, subscription(planId, customerId, null), null).expectStatus(201);
    }

    static Stream<String> validCustomerIds() {
        return Stream.of("c", "c".repeat(64), "Az09_-", "cus_42");
    }

    @Test
    void S3_customerIdIsStoredAsGiven() {
        String planId = m7();
        var created = api.postSubscription(client, subscription(planId, "CuS_42", null), null)
                .expectStatus(201);

        var charges = api.billingDates(client, "2026-03-05", "2026-04-30", "CuS_42").expectStatus(200);
        assertThat(created.text("customerId")).isEqualTo("CuS_42");
        assertThat(charges.body().get("charges").get(0).get("customerId").asText()).isEqualTo("CuS_42");
    }

    @Test
    void S5_2_planIdIsRequired() {
        api.postSubscription(client, subscription(null, "cus_42", null), null)
                .expectValidationError("planId");
    }

    @Test
    void S5_subscriptionFieldsAreCheckedInTheOrderListed() {
        api.postSubscription(client, subscription(null, "bad id!", "2026-03-06"), null)
                .expectValidationError("planId");
        api.postSubscription(client, subscription("pln_0000000000", "bad id!", "2026-03-06"), null)
                .expectValidationError("customerId");
        api.postSubscription(client, subscription("pln_0000000000", "cus_42", "2026-03-06"), null)
                .expectValidationError("startDate");
    }

    @Test
    void S5_validationComesBeforeNotFound() {
        api.subscribe(client, "pln_0000000000", "bad id!", null).expectValidationError("customerId");
    }

    @Test
    void S5_2_unknownPlanReturnsPlanNotFound() {
        api.subscribe(client, "pln_0000000000", "cus_42", null).expectError(404, "PLAN_NOT_FOUND");
    }

    @Test
    void S5_2_startDateOfTodayIsAcceptedAndDefaultsToToday() {
        String planId = m0();

        var explicit = api.subscribe(client, planId, "cus_1", "2026-03-05").expectStatus(201);
        var defaulted = api.subscribe(client, planId, "cus_2", null).expectStatus(201);

        assertThat(explicit.text("startDate")).isEqualTo("2026-03-05");
        assertThat(defaulted.text("startDate")).isEqualTo("2026-03-05");
    }

    // ---- GET /subscriptions/{id}, POST cancel ------------------------------------------------

    @Test
    void S5_3_asOfBeforeStartDateIsRejected() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        api.view(client, subscriptionId, "2026-01-23").expectValidationError("asOf");
        api.view(client, subscriptionId, "2026-01-24").expectStatus(200);
    }

    @Test
    void S5_3_unparseableAsOfIsRejected() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        api.view(client, subscriptionId, "yesterday").expectValidationError("asOf");
        api.view(client, subscriptionId, "2026-02-30").expectValidationError("asOf");
        api.view(client, subscriptionId, "2026-3-5").expectValidationError("asOf");
    }

    @Test
    void S5_cancelModeMustBePresentAndKnown() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        api.send("POST", client, "/subscriptions/" + subscriptionId + "/cancel", json(), null)
                .expectValidationError("mode");
        api.cancel(client, subscriptionId, "SOMETIME").expectValidationError("mode");
        api.cancel(client, subscriptionId, "immediate").expectValidationError("mode");
    }

    @Test
    void S5_validationComesBeforeNotFoundOnCancel() {
        api.cancel(client, "sub_0000000000", "SOMETIME").expectValidationError("mode");
        api.cancel(client, "sub_0000000000", "IMMEDIATE").expectError(404, "SUBSCRIPTION_NOT_FOUND");
    }

    @Test
    void S5_unknownSubscriptionIdIsNotFound() {
        api.view(client, "sub_0000000000", null).expectError(404, "SUBSCRIPTION_NOT_FOUND");
    }

    // ---- GET /billing-dates ------------------------------------------------------------------

    @Test
    void S5_5_missingFromOrToIsRejectedNamingTheField() {
        api.billingDates(client, null, "2026-03-31", null).expectValidationError("from");
        api.billingDates(client, "2026-03-01", null, null).expectValidationError("to");
    }

    @Test
    void S5_5_unparseableDatesAreRejectedNamingTheField() {
        api.billingDates(client, "march", "2026-03-31", null).expectValidationError("from");
        api.billingDates(client, "2026-03-01", "2026-13-01", null).expectValidationError("to");
    }

    @Test
    void S5_5_toBeforeFromIsRejected() {
        api.billingDates(client, "2026-03-02", "2026-03-01", null).expectValidationError("to");
    }

    @Test
    void S5_5_aSingleDayWindowIsAccepted() {
        api.billingDates(client, "2026-03-01", "2026-03-01", null).expectStatus(200);
    }
}
