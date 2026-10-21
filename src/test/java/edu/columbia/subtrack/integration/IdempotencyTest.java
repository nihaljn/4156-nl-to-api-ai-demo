package edu.columbia.subtrack.integration;

import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.support.ApiTestBase;
import edu.columbia.subtrack.support.SubTrackApi;
import edu.columbia.subtrack.support.SubTrackApi.Response;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** SPEC section 2, "Idempotency" (AT-11 and the rest of the paragraph). */
class IdempotencyTest extends ApiTestBase {

    private int chargesInQ1Window() {
        return api.billingDates(client, "2026-02-01", "2026-03-31", null)
                .expectStatus(200).chargeDates().size();
    }

    @Test
    void AT11_S2_sameKeyAndBodyReplaysTheStoredResponse() {
        String planId = m7();

        Response first = api.subscribe(client, planId, "cus_42", "2026-01-24", "key-1").expectStatus(201);
        Response replay = api.subscribe(client, planId, "cus_42", "2026-01-24", "key-1").expectStatus(201);

        assertThat(replay.raw()).isEqualTo(first.raw());
        assertThat(chargesInQ1Window()).as("one stored subscription bills Feb and Mar once").isEqualTo(2);
    }

    @Test
    void AT11_S2_sameKeyWithADifferentBodyIsAMismatch() {
        String planId = m7();
        api.subscribe(client, planId, "cus_42", "2026-01-24", "key-1").expectStatus(201);

        api.subscribe(client, planId, "cus_43", "2026-01-24", "key-1")
                .expectError(409, "IDEMPOTENCY_MISMATCH");

        assertThat(chargesInQ1Window()).isEqualTo(2);
    }

    @Test
    void S2_replayReturnsTheStoredBodyNotAFreshComputation() {
        String planId = m7();
        Response first = api.subscribe(client, planId, "cus_42", "2026-01-24", "key-1").expectStatus(201);

        clock.advance(Duration.ofDays(30));
        Response replay = api.subscribe(client, planId, "cus_42", "2026-01-24", "key-1").expectStatus(201);

        assertThat(replay.raw()).isEqualTo(first.raw());
    }

    @Test
    void S2_planCreationReplaysUnderTheSameKey() {
        Object body = SubTrackApi.planBody("Pro monthly", 999, "USD", "MONTH", 7);

        Response first = api.postPlan(client, body, "plan-key").expectStatus(201);
        Response replay = api.postPlan(client, body, "plan-key").expectStatus(201);

        assertThat(replay.raw()).isEqualTo(first.raw());
        assertThat(replay.text("planId")).isEqualTo(first.text("planId"));
    }

    @Test
    void S2_planCreationWithSameKeyAndDifferentBodyIsAMismatch() {
        api.postPlan(client, SubTrackApi.planBody("Pro monthly", 999, "USD", "MONTH", 7), "plan-key")
                .expectStatus(201);

        api.postPlan(client, SubTrackApi.planBody("Pro monthly", 1999, "USD", "MONTH", 7), "plan-key")
                .expectError(409, "IDEMPOTENCY_MISMATCH");
    }

    @Test
    void S2_theSameKeyFromAnotherClientIsIndependent() {
        String otherClient = "client-other-" + client;
        Object body = SubTrackApi.planBody("Pro monthly", 999, "USD", "MONTH", 7);
        Object otherBody = SubTrackApi.planBody("Team monthly", 4999, "USD", "MONTH", 0);

        Response mine = api.postPlan(client, body, "shared-key").expectStatus(201);
        Response theirs = api.postPlan(otherClient, otherBody, "shared-key").expectStatus(201);

        assertThat(theirs.text("planId")).isNotEqualTo(mine.text("planId"));
        assertThat(theirs.text("name")).isEqualTo("Team monthly");
    }

    @Test
    void S2_requestsWithoutAKeyAreNotDeduplicated() {
        Object body = SubTrackApi.planBody("Pro monthly", 999, "USD", "MONTH", 7);
        api.postPlan(client, body, null).expectStatus(201);

        api.postPlan(client, body, null).expectError(409, "PLAN_NAME_TAKEN");
    }

    @Test
    void S2_keyLengthIsOneToOneHundredCharacters() {
        Object body = SubTrackApi.planBody("Pro monthly", 999, "USD", "MONTH", 7);

        api.postPlan(client, SubTrackApi.planBody("One", 999, "USD", "MONTH", 0), "k").expectStatus(201);
        api.postPlan(client, SubTrackApi.planBody("Hundred", 999, "USD", "MONTH", 0), "k".repeat(100))
                .expectStatus(201);
        api.postPlan(client, body, "k".repeat(101)).expectError(400, "VALIDATION_ERROR");
    }
}
