package edu.columbia.subtrack.integration;

import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.support.ApiTestBase;
import edu.columbia.subtrack.support.SubTrackApi;
import edu.columbia.subtrack.support.SubTrackApi.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** SPEC section 2: "Two concurrent requests with the same key must not both write." */
class ConcurrencyTest extends ApiTestBase {

    private static final int THREADS = 10;

    private static List<Response> race(Callable<Response> request) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CyclicBarrier startTogether = new CyclicBarrier(THREADS);
        try {
            List<Future<Response>> pending = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                pending.add(pool.submit(() -> {
                    startTogether.await(10, TimeUnit.SECONDS);
                    return request.call();
                }));
            }
            List<Response> responses = new ArrayList<>();
            for (Future<Response> future : pending) {
                responses.add(future.get(30, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void AT12_S2_tenConcurrentSubscribesWithOneKeyStoreOneSubscription() throws Exception {
        String planId = m7();

        List<Response> responses = race(
                () -> api.subscribe(client, planId, "cus_42", "2026-01-24", "same-key"));

        assertThat(responses).allSatisfy(response -> response.expectStatus(201));
        assertThat(responses.stream().map(response -> response.text("subscriptionId")).distinct())
                .hasSize(1);
        assertThat(api.billingDates(client, "2026-02-01", "2026-03-31", null)
                .expectStatus(200).chargeDates())
                .as("one stored subscription bills Feb and Mar once")
                .hasSize(2);
    }

    @Test
    void S2_tenConcurrentPlanCreationsWithOneKeyStoreOnePlan() throws Exception {
        Object body = SubTrackApi.planBody("Pro monthly", 999, "USD", "MONTH", 7);

        List<Response> responses = race(() -> api.postPlan(client, body, "same-key"));

        assertThat(responses).allSatisfy(response -> response.expectStatus(201));
        assertThat(responses.stream().map(response -> response.text("planId")).distinct()).hasSize(1);
        api.postPlan(client, body, null).expectError(409, "PLAN_NAME_TAKEN");
    }
}
