package edu.columbia.subtrack.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.columbia.subtrack.service.BillingDatesService;
import edu.columbia.subtrack.service.PlanService;
import edu.columbia.subtrack.service.SubscriptionService;
import edu.columbia.subtrack.support.TestClockConfig;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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

    static Stream<Arguments> requestsWithoutUsableKey() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/plans", null),
                Arguments.of(HttpMethod.POST, "/plans", "  "),
                Arguments.of(HttpMethod.POST, "/subscriptions", null),
                Arguments.of(HttpMethod.POST, "/subscriptions", ""),
                Arguments.of(HttpMethod.GET, "/subscriptions/sub_0123456789", null),
                Arguments.of(HttpMethod.GET, "/subscriptions/sub_0123456789", " "),
                Arguments.of(HttpMethod.POST, "/subscriptions/sub_0123456789/cancel", null),
                Arguments.of(HttpMethod.POST, "/subscriptions/sub_0123456789/cancel", " "),
                Arguments.of(HttpMethod.GET, "/billing-dates?from=2026-03-01&to=2026-03-31", null),
                Arguments.of(HttpMethod.GET, "/billing-dates?from=2026-03-01&to=2026-03-31", ""));
    }

    @ParameterizedTest
    @MethodSource("requestsWithoutUsableKey")
    void section2_requestWithoutClientKeyIsUnauthenticated(HttpMethod method, String path, String key)
            throws Exception {
        // AT-16
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, path);
        if (method == HttpMethod.POST) {
            request.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        if (key != null) {
            request.header("X-Client-Key", key);
        }

        mvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }
}
