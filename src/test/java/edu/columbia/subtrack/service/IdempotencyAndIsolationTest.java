package edu.columbia.subtrack.service;

import static edu.columbia.subtrack.support.Http.CLIENT_A;
import static edu.columbia.subtrack.support.Http.CLIENT_B;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import edu.columbia.subtrack.support.Http;
import edu.columbia.subtrack.support.TestClockConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class IdempotencyAndIsolationTest {

    private static final String MARCH = "/billing-dates?from=2026-03-01&to=2026-03-31";

    @Autowired
    private MockMvc mvc;

    @Test
    void section2_repeatedIdempotencyKeyReplaysAndDifferentBodyIsMismatch() throws Exception {
        // AT-11
        String planId = Http.createM7(mvc, CLIENT_A);
        String body = "{\"planId\":\"" + planId + "\",\"customerId\":\"cus_42\"}";
        String other = "{\"planId\":\"" + planId + "\",\"customerId\":\"cus_43\"}";

        String first = Http.postWithKey(mvc, CLIENT_A, "/subscriptions", body, "key-1")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = Http.postWithKey(mvc, CLIENT_A, "/subscriptions", body, "key-1")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertEquals(first, second);
        Http.getAs(mvc, CLIENT_A, MARCH + "&customerId=cus_42")
                .andExpect(jsonPath("$.charges", hasSize(1)));
        Http.postWithKey(mvc, CLIENT_A, "/subscriptions", other, "key-1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_MISMATCH"));
    }

    @Test
    void section2_concurrentRequestsWithSameIdempotencyKeyWriteOnce() throws Exception {
        // AT-12
        int threads = 10;
        String planId = Http.createM7(mvc, CLIENT_A);
        String body = "{\"planId\":\"" + planId + "\",\"customerId\":\"cus_42\"}";
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<String> call = () -> {
                go.await();
                String response = Http.postWithKey(mvc, CLIENT_A, "/subscriptions", body, "same-key")
                        .andReturn().getResponse().getContentAsString();
                return JsonPath.read(response, "$.subscriptionId");
            };
            results.add(pool.submit(call));
        }
        go.countDown();

        Set<String> ids = new HashSet<>();
        for (Future<String> result : results) {
            ids.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(1, ids.size());
        Http.getAs(mvc, CLIENT_A, MARCH)
                .andExpect(jsonPath("$.charges", hasSize(1)));
    }

    @Test
    void section2_anotherClientCannotSeeSubscription() throws Exception {
        // AT-13
        String planId = Http.createM7(mvc, CLIENT_A);
        String id = Http.subscribeOk(mvc, CLIENT_A, planId, "cus_42", null);

        Http.getAs(mvc, CLIENT_B, "/subscriptions/" + id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SUBSCRIPTION_NOT_FOUND"));
        Http.getAs(mvc, CLIENT_B, MARCH)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.charges", hasSize(0)));
    }
}
