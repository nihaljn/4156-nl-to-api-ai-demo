package edu.columbia.subtrack.flow;

import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.support.ApiClient;
import edu.columbia.subtrack.support.TestClock;
import edu.columbia.subtrack.support.TestClockConfig;
import java.time.LocalDate;
import java.util.List;
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
class SubscriptionLifecycleTest {

    private static final LocalDate JAN_24 = LocalDate.of(2026, 1, 24);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestClock clock;

    private ApiClient client() {
        return new ApiClient(mvc, "client-a");
    }

    /** AT-4's subscription: M7 starting 2026-01-24. */
    private String m7Subscription(final ApiClient client) throws Exception {
        final String planId = client.createMonthlyPlan("M7", 7);
        return client.subscribeOk(planId, "cus_42", JAN_24);
    }

    private static void assertStatus(final MvcResult result, final int expected) {
        assertThat(result.getResponse().getStatus()).isEqualTo(expected);
    }

    // AT-5
    @Test
    void R3_R6_R7_cancelAtPeriodEndSetsEndsOnToCurrentPeriodEnd() throws Exception {
        final ApiClient client = client();
        final String id = m7Subscription(client);

        final MvcResult canceled = client.cancel(id, "AT_PERIOD_END");

        assertStatus(canceled, 200);
        assertThat(ApiClient.json(canceled, "$.endsOn")).isEqualTo("2026-03-31");
        assertThat(ApiClient.json(canceled, "$.cancelAtPeriodEnd")).isEqualTo(true);
        assertThat(ApiClient.json(client.getSubscription(id, LocalDate.of(2026, 3, 31)), "$.status"))
            .isEqualTo("CANCELED");
        final MvcResult dates = client.billingDates(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assertThat((List<?>) ApiClient.json(dates, "$.charges")).isEmpty();
    }

    // AT-6
    @Test
    void R3_R7_cancelAtPeriodEndDuringTrialEndsOnFirstBillingDate() throws Exception {
        clock.setDate(LocalDate.of(2026, 1, 26));
        final ApiClient client = client();
        final String id = m7Subscription(client);

        final MvcResult canceled = client.cancel(id, "AT_PERIOD_END");

        assertThat(ApiClient.json(canceled, "$.endsOn")).isEqualTo("2026-01-31");
        final MvcResult dates = client.billingDates(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        assertThat((List<?>) ApiClient.json(dates, "$.charges")).isEmpty();
    }

    // AT-7
    @Test
    void R7_immediateCancelTwiceIsIdempotent() throws Exception {
        final ApiClient client = client();
        final String id = m7Subscription(client);

        for (int attempt = 0; attempt < 2; attempt++) {
            final MvcResult result = client.cancel(id, "IMMEDIATE");

            assertStatus(result, 200);
            assertThat(ApiClient.json(result, "$.status")).isEqualTo("CANCELED");
            assertThat(ApiClient.json(result, "$.endsOn")).isEqualTo("2026-03-05");
        }
    }

    // AT-8
    @Test
    void R7_cancelAtPeriodEndOnCanceledSubscriptionIsConflict() throws Exception {
        final ApiClient client = client();
        final String id = m7Subscription(client);
        client.cancel(id, "IMMEDIATE");

        final MvcResult result = client.cancel(id, "AT_PERIOD_END");

        assertStatus(result, 409);
        assertThat(ApiClient.json(result, "$.error")).isEqualTo("ALREADY_CANCELED");
    }

    // AT-9
    @Test
    void R7_immediateCancelOverridesScheduledPeriodEndCancel() throws Exception {
        final ApiClient client = client();
        final String id = m7Subscription(client);
        client.cancel(id, "AT_PERIOD_END");
        clock.setDate(LocalDate.of(2026, 3, 10));

        final MvcResult result = client.cancel(id, "IMMEDIATE");

        assertThat(ApiClient.json(result, "$.endsOn")).isEqualTo("2026-03-10");
        assertThat(ApiClient.json(result, "$.status")).isEqualTo("CANCELED");
    }

    // AT-10
    @Test
    void R8_secondLiveSubscriptionForSameCustomerAndPlanIsRejected() throws Exception {
        final ApiClient client = client();
        final String planId = client.createMonthlyPlan("M7", 7);
        final String firstId = client.subscribeOk(planId, "cus_42", null);

        final MvcResult duplicate = client.subscribe(planId, "cus_42", null, null);

        assertStatus(duplicate, 409);
        assertThat(ApiClient.json(duplicate, "$.error")).isEqualTo("ALREADY_SUBSCRIBED");
        assertThat(ApiClient.json(duplicate, "$.existingSubscriptionId")).isEqualTo(firstId);

        client.cancel(firstId, "IMMEDIATE");

        assertStatus(client.subscribe(planId, "cus_42", null, null), 201);
    }
}
