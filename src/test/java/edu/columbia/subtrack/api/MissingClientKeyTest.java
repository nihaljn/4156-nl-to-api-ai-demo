package edu.columbia.subtrack.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.columbia.subtrack.service.BillingDatesService;
import edu.columbia.subtrack.service.PlanService;
import edu.columbia.subtrack.service.SubscriptionService;
import edu.columbia.subtrack.support.TestClockConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest
@Import(TestClockConfig.class)
class MissingClientKeyTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PlanService planService;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private BillingDatesService billingDatesService;

    // AT-16: one case per endpoint, plus a blank key.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "POST|/plans|",
        "POST|/subscriptions|",
        "GET|/subscriptions/sub_0123456789|",
        "POST|/subscriptions/sub_0123456789/cancel|",
        "GET|/billing-dates?from=2026-03-01&to=2026-03-31|",
        "POST|/plans|   "
    })
    void section2_missingClientKeyIsUnauthenticated(final String method, final String path, final String key)
            throws Exception {
        final MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), path)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}");
        if (key != null) {
            request.header("X-Client-Key", key);
        }

        mvc.perform(request)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }
}
