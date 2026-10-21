package edu.columbia.subtrack.integration;

import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.support.ApiTestBase;
import edu.columbia.subtrack.support.SubTrackApi.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** SPEC section 2, "Isolation" and "Client identity". */
class IsolationTest extends ApiTestBase {

    private String other() {
        return "client-other-" + client;
    }

    @Test
    void AT13_S2_anotherClientsSubscriptionIsNotFoundAndItsChargesAreHidden() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");
        String intruder = other();

        api.view(intruder, subscriptionId, null).expectError(404, "SUBSCRIPTION_NOT_FOUND");

        Response charges = api.billingDates(intruder, "2026-02-01", "2026-03-31", null).expectStatus(200);
        assertThat(charges.chargeDates()).isEmpty();
    }

    @Test
    void S2_anotherClientsSubscriptionBehavesLikeOneThatDoesNotExist() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        Response foreign = api.view(other(), subscriptionId, null);
        Response missing = api.view(other(), "sub_0000000000", null);

        assertThat(foreign.status()).isEqualTo(missing.status());
        assertThat(foreign.text("error")).isEqualTo(missing.text("error"));
        assertThat(foreign.has("field")).isEqualTo(missing.has("field"));
    }

    @Test
    void S2_anotherClientCannotCancelMySubscription() {
        String subscriptionId = api.subscribeOk(client, m7(), "cus_42", "2026-01-24");

        api.cancel(other(), subscriptionId, "IMMEDIATE").expectError(404, "SUBSCRIPTION_NOT_FOUND");

        assertThat(api.view(client, subscriptionId, null).text("status")).isEqualTo("ACTIVE");
    }

    @Test
    void S2_anotherClientsPlanIsNotFoundWhenSubscribing() {
        String planId = m7();

        api.subscribe(other(), planId, "cus_42", null).expectError(404, "PLAN_NOT_FOUND");
    }

    @Test
    void S2_planNamesAreUniquePerClientNotGlobally() {
        api.createPlan(client, "Pro", "MONTH", 999, 0);

        api.createPlan(other(), "Pro", "MONTH", 999, 0);
    }

    @Test
    void S2_theSameCustomerIdCanSubscribeUnderTwoClients() {
        api.subscribeOk(client, m7(), "cus_42", null);
        String otherPlan = api.createPlan(other(), "M7", "MONTH", 999, 7);

        api.subscribe(other(), otherPlan, "cus_42", null).expectStatus(201);
    }

    @Test
    void S2_billingDatesListOnlyTheCallersSubscriptions() {
        String mine = api.subscribeOk(client, m0(), "cus_1", "2026-01-31");
        String otherPlan = api.createPlan(other(), "M0", "MONTH", 999, 0);
        String theirs = api.subscribeOk(other(), otherPlan, "cus_2", "2026-01-31");

        List<String> mineSeen = subscriptionIdsInCharges(client);
        List<String> theirsSeen = subscriptionIdsInCharges(other());

        assertThat(mineSeen).isNotEmpty().containsOnly(mine);
        assertThat(theirsSeen).isNotEmpty().containsOnly(theirs);
    }

    @Test
    void S2_anyNonBlankClientKeyIsAccepted() {
        String unregistered = "never-registered-" + "x".repeat(150);

        api.createPlan(unregistered, "Pro", "MONTH", 999, 0);
    }

    private List<String> subscriptionIdsInCharges(String clientKey) {
        Response charges = api.billingDates(clientKey, "2026-02-01", "2026-03-31", null).expectStatus(200);
        List<String> ids = new ArrayList<>();
        charges.body().get("charges").forEach(charge -> ids.add(charge.get("subscriptionId").asText()));
        return ids;
    }
}
