package edu.columbia.subtrack.flow;

import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.support.ApiClient;
import edu.columbia.subtrack.support.TestClockConfig;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class IdempotencyAndIsolationTest {

    private static final int THREADS = 10;

    /** A subscription starting today on M7 first bills on this day, so a one-day window counts them. */
    private static final LocalDate FIRST_BILLING = LocalDate.of(2026, 3, 12);

    @Autowired
    private MockMvc mvc;

    private ApiClient client(final String key) {
        return new ApiClient(mvc, key);
    }

    private static int chargeCount(final ApiClient client) throws Exception {
        final MvcResult dates = client.billingDates(FIRST_BILLING, FIRST_BILLING);
        return ((List<?>) ApiClient.json(dates, "$.charges")).size();
    }

    // AT-11
    @Test
    void section2_repeatedIdempotencyKeyReplaysAndDifferentBodyIsMismatch() throws Exception {
        final ApiClient client = client("client-a");
        final String planId = client.createMonthlyPlan("M7", 7);

        final MvcResult first = client.subscribe(planId, "cus_42", null, "key-1");
        final MvcResult second = client.subscribe(planId, "cus_42", null, "key-1");

        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        assertThat(second.getResponse().getStatus()).isEqualTo(201);
        assertThat(second.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());
        assertThat(chargeCount(client)).isEqualTo(1);

        final MvcResult mismatch = client.subscribe(planId, "cus_43", null, "key-1");

        assertThat(mismatch.getResponse().getStatus()).isEqualTo(409);
        assertThat(ApiClient.json(mismatch, "$.error")).isEqualTo("IDEMPOTENCY_MISMATCH");
    }

    // AT-12
    @Test
    void section2_concurrentRequestsWithSameIdempotencyKeyWriteOnce() throws Exception {
        final ApiClient client = client("client-a");
        final String planId = client.createMonthlyPlan("M7", 7);
        final CountDownLatch go = new CountDownLatch(1);
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        final List<Future<String>> results = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                final Callable<String> task = () -> {
                    go.await();
                    final MvcResult result = client.subscribe(planId, "cus_42", null, "key-1");
                    return (String) ApiClient.json(result, "$.subscriptionId");
                };
                results.add(pool.submit(task));
            }
            go.countDown();

            final List<String> ids = new ArrayList<>();
            for (final Future<String> result : results) {
                ids.add(result.get());
            }

            assertThat(ids).hasSize(THREADS).containsOnly(ids.get(0));
            assertThat(chargeCount(client)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    // AT-13
    @Test
    void section2_anotherClientCannotSeeSubscription() throws Exception {
        final ApiClient clientA = client("client-a");
        final ApiClient clientB = client("client-b");
        final String subscriptionId = clientA.subscribeOk(clientA.createMonthlyPlan("M7", 7), "cus_42", null);
        assertThat(chargeCount(clientA)).isEqualTo(1);

        final MvcResult lookup = clientB.getSubscription(subscriptionId, null);

        assertThat(lookup.getResponse().getStatus()).isEqualTo(404);
        assertThat(ApiClient.json(lookup, "$.error")).isEqualTo("SUBSCRIPTION_NOT_FOUND");
        assertThat(chargeCount(clientB)).isZero();
    }
}
