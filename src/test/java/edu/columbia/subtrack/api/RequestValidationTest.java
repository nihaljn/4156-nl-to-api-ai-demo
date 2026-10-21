package edu.columbia.subtrack.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.columbia.subtrack.service.BillingDatesService;
import edu.columbia.subtrack.service.PlanService;
import edu.columbia.subtrack.service.SubscriptionService;
import edu.columbia.subtrack.support.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(TestClockConfig.class)
class RequestValidationTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PlanService planService;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private BillingDatesService billingDatesService;

    // AT-14
    @Test
    void section5_5_billingDatesWindowIsCappedAt92Days() throws Exception {
        mvc.perform(get("/billing-dates?from=2026-03-01&to=2026-06-01").header("X-Client-Key", "client-a"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.field").value("to"));

        mvc.perform(get("/billing-dates?from=2026-03-01&to=2026-05-31").header("X-Client-Key", "client-a"))
            .andExpect(status().isOk());
    }

    // AT-15
    @Test
    void section5_2_startDateAfterTodayIsRejected() throws Exception {
        mvc.perform(post("/subscriptions")
                .header("X-Client-Key", "client-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":\"pln_0123456789\",\"customerId\":\"cus_42\",\"startDate\":\"2026-03-06\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.field").value("startDate"));
    }
}
