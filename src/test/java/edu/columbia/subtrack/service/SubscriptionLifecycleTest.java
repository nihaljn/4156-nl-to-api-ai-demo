package edu.columbia.subtrack.service;

import static edu.columbia.subtrack.support.Http.CLIENT_A;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.columbia.subtrack.support.Http;
import edu.columbia.subtrack.support.TestClock;
import edu.columbia.subtrack.support.TestClockConfig;
import java.time.LocalDate;
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
class SubscriptionLifecycleTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestClock clock;

    private String m7Subscription(String startDate) throws Exception {
        String planId = Http.createM7(mvc, CLIENT_A);
        return Http.subscribeOk(mvc, CLIENT_A, planId, "cus_42", startDate);
    }

    @Test
    void R3_R6_R7_cancelAtPeriodEndSetsEndsOnToCurrentPeriodEnd() throws Exception {
        // AT-5
        String id = m7Subscription("2026-01-24");

        Http.cancel(mvc, CLIENT_A, id, "AT_PERIOD_END")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endsOn").value("2026-03-31"))
                .andExpect(jsonPath("$.cancelAtPeriodEnd").value(true));
        Http.getAs(mvc, CLIENT_A, "/subscriptions/" + id + "?asOf=2026-03-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
        Http.getAs(mvc, CLIENT_A, "/billing-dates?from=2026-03-01&to=2026-03-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.charges", hasSize(0)));
    }

    @Test
    void R3_R7_cancelAtPeriodEndDuringTrialEndsOnFirstBillingDate() throws Exception {
        // AT-6
        clock.setToday(LocalDate.of(2026, 1, 26));
        String id = m7Subscription("2026-01-24");

        Http.cancel(mvc, CLIENT_A, id, "AT_PERIOD_END")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endsOn").value("2026-01-31"));
        Http.getAs(mvc, CLIENT_A, "/billing-dates?from=2026-01-01&to=2026-03-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.charges", hasSize(0)));
    }

    @Test
    void R7_immediateCancelTwiceIsIdempotent() throws Exception {
        // AT-7
        String id = m7Subscription("2026-01-24");

        for (int attempt = 0; attempt < 2; attempt++) {
            Http.cancel(mvc, CLIENT_A, id, "IMMEDIATE")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELED"))
                    .andExpect(jsonPath("$.endsOn").value("2026-03-05"));
        }
    }

    @Test
    void R7_cancelAtPeriodEndOnCanceledSubscriptionIsConflict() throws Exception {
        // AT-8: AT-7's end state
        String id = m7Subscription("2026-01-24");
        Http.cancel(mvc, CLIENT_A, id, "IMMEDIATE").andExpect(status().isOk());

        Http.cancel(mvc, CLIENT_A, id, "AT_PERIOD_END")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_CANCELED"));
    }

    @Test
    void R7_immediateCancelOverridesScheduledPeriodEndCancel() throws Exception {
        // AT-9: AT-5's end state, then clock 03-10
        String id = m7Subscription("2026-01-24");
        Http.cancel(mvc, CLIENT_A, id, "AT_PERIOD_END").andExpect(status().isOk());
        clock.setToday(LocalDate.of(2026, 3, 10));

        Http.cancel(mvc, CLIENT_A, id, "IMMEDIATE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endsOn").value("2026-03-10"))
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void R8_secondLiveSubscriptionForSameCustomerAndPlanIsRejected() throws Exception {
        // AT-10
        String planId = Http.createM7(mvc, CLIENT_A);
        String first = Http.subscribeOk(mvc, CLIENT_A, planId, "cus_42", null);

        Http.subscribe(mvc, CLIENT_A, planId, "cus_42", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_SUBSCRIBED"))
                .andExpect(jsonPath("$.existingSubscriptionId").value(first));
        Http.cancel(mvc, CLIENT_A, first, "IMMEDIATE").andExpect(status().isOk());
        Http.subscribe(mvc, CLIENT_A, planId, "cus_42", null)
                .andExpect(status().isCreated());
    }
}
