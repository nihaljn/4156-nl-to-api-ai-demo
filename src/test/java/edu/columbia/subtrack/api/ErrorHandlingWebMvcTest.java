package edu.columbia.subtrack.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.columbia.subtrack.service.BillingDatesService;
import edu.columbia.subtrack.service.PlanService;
import edu.columbia.subtrack.service.SubscriptionService;
import edu.columbia.subtrack.support.TestClockConfig;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Status codes and error shape (SPEC sections 2 and 6) with every service replaced by a double
 * that throws whatever the test chooses. Assumes the controllers depend only on the three
 * services below and the Clock, and that services signal failures with {@code ApiException}.
 */
@WebMvcTest
@Import({TestClockConfig.class, ErrorHandlingWebMvcTest.ServiceDoubles.class})
class ErrorHandlingWebMvcTest {

    private static final String PLAN_BODY =
            "{\"name\":\"Pro monthly\",\"priceCents\":999,\"currency\":\"USD\","
                    + "\"interval\":\"MONTH\",\"trialDays\":7}";
    private static final String SUBSCRIBE_BODY =
            "{\"planId\":\"pln_3k8d0a2zpr\",\"customerId\":\"cus_42\",\"startDate\":\"2026-01-24\"}";
    private static final String CANCEL_BODY = "{\"mode\":\"AT_PERIOD_END\"}";

    private static final AtomicReference<Throwable> FAILURE = new AtomicReference<>();

    private static final Answer<Object> THROW_CONFIGURED_FAILURE = invocation -> {
        Throwable failure = FAILURE.get();
        if (failure == null || invocation.getMethod().getDeclaringClass() == Object.class) {
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        }
        throw failure;
    };

    @TestConfiguration(proxyBeanMethods = false)
    static class ServiceDoubles {

        @Bean
        PlanService planService() {
            return Mockito.mock(PlanService.class, THROW_CONFIGURED_FAILURE);
        }

        @Bean
        SubscriptionService subscriptionService() {
            return Mockito.mock(SubscriptionService.class, THROW_CONFIGURED_FAILURE);
        }

        @Bean
        BillingDatesService billingDatesService() {
            return Mockito.mock(BillingDatesService.class, THROW_CONFIGURED_FAILURE);
        }
    }

    /** One well-formed call per endpoint. */
    record Call(String label, String method, String path, String body) {

        MockHttpServletRequestBuilder build(String clientKey) {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                    .request(HttpMethod.valueOf(method), path)
                    .accept(MediaType.APPLICATION_JSON);
            if (clientKey != null) {
                request.header("X-Client-Key", clientKey);
            }
            if (body != null) {
                request.contentType(MediaType.APPLICATION_JSON).content(body);
            }
            return request;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    static final Call POST_PLANS = new Call("POST /plans", "POST", "/plans", PLAN_BODY);
    static final Call POST_SUBSCRIPTIONS =
            new Call("POST /subscriptions", "POST", "/subscriptions", SUBSCRIBE_BODY);
    static final Call GET_SUBSCRIPTION =
            new Call("GET /subscriptions/{id}", "GET", "/subscriptions/sub_9f2k1m0xqa", null);
    static final Call POST_CANCEL = new Call("POST /subscriptions/{id}/cancel", "POST",
            "/subscriptions/sub_9f2k1m0xqa/cancel", CANCEL_BODY);
    static final Call GET_BILLING_DATES = new Call("GET /billing-dates", "GET",
            "/billing-dates?from=2026-02-01&to=2026-03-31", null);
    static final Call GET_UNKNOWN_PATH = new Call("GET /no-such-endpoint", "GET", "/no-such-endpoint", null);

    static Stream<Call> endpoints() {
        return Stream.of(POST_PLANS, POST_SUBSCRIPTIONS, GET_SUBSCRIPTION, POST_CANCEL, GET_BILLING_DATES);
    }

    static Stream<Call> endpointsAndUnknownPath() {
        return Stream.concat(endpoints(), Stream.of(GET_UNKNOWN_PATH));
    }

    static Stream<Call> endpointsWithBody() {
        return Stream.of(POST_PLANS, POST_SUBSCRIPTIONS, POST_CANCEL);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void serviceFailsUnlessTheTestSaysOtherwise() {
        FAILURE.set(new IllegalStateException("service must not be reached"));
    }

    private JsonNode errorBody(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    // ---- 401 ---------------------------------------------------------------------------------

    @ParameterizedTest(name = "AT-16 {0}")
    @MethodSource("endpointsAndUnknownPath")
    void AT16_requestWithoutClientKeyReturns401(Call call) throws Exception {
        mvc.perform(call.build(null))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @ParameterizedTest(name = "blank key [{0}]")
    @ValueSource(strings = {"", " ", "   "})
    void S2_blankClientKeyReturns401(String blankKey) throws Exception {
        mvc.perform(POST_PLANS.build(blankKey))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void S5_keyIsCheckedBeforeTheBody() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/plans")
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    // ---- 400 MALFORMED_REQUEST ---------------------------------------------------------------

    @ParameterizedTest(name = "{0} with invalid JSON")
    @MethodSource("endpointsWithBody")
    void S6_invalidJsonBodyReturnsMalformedRequest(Call call) throws Exception {
        mvc.perform(MockMvcRequestBuilders.request(HttpMethod.POST, call.path())
                        .header("X-Client-Key", "test-client")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @ParameterizedTest(name = "{0} with no body")
    @MethodSource("endpointsWithBody")
    void S6_missingBodyReturnsMalformedRequest(Call call) throws Exception {
        mvc.perform(MockMvcRequestBuilders.request(HttpMethod.POST, call.path())
                        .header("X-Client-Key", "test-client")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void S2_unknownRequestFieldsAreIgnored() throws Exception {
        FAILURE.set(new ApiException("PLAN_NAME_TAKEN", "reached the service", null));
        String bodyWithExtras = PLAN_BODY.replace("}", ",\"colour\":\"blue\",\"nested\":{\"a\":1}}");

        mvc.perform(MockMvcRequestBuilders.post("/plans").header("X-Client-Key", "test-client")
                        .contentType(MediaType.APPLICATION_JSON).content(bodyWithExtras))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PLAN_NAME_TAKEN"));
    }

    // ---- 404 NOT_FOUND -----------------------------------------------------------------------

    @Test
    void S6_pathThatMatchesNoEndpointReturnsNotFound() throws Exception {
        mvc.perform(GET_UNKNOWN_PATH.build("test-client"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ---- error mapping and shape -------------------------------------------------------------

    static Stream<Arguments> serviceErrorCodes() {
        return Stream.of(
                Arguments.of("VALIDATION_ERROR", 400),
                Arguments.of("MALFORMED_REQUEST", 400),
                Arguments.of("PLAN_NOT_FOUND", 404),
                Arguments.of("SUBSCRIPTION_NOT_FOUND", 404),
                Arguments.of("PLAN_NAME_TAKEN", 409),
                Arguments.of("ALREADY_SUBSCRIBED", 409),
                Arguments.of("ALREADY_CANCELED", 409),
                Arguments.of("IDEMPOTENCY_MISMATCH", 409));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("serviceErrorCodes")
    void S6_errorCodeMapsToItsStatus(String code, int expectedStatus) throws Exception {
        FAILURE.set(new ApiException(code, "explained in words", null));

        mvc.perform(POST_PLANS.build("test-client"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.error").value(code))
                .andExpect(jsonPath("$.message").value("explained in words"));
    }

    @Test
    void S2_errorBodyCarriesFieldWhenOneFieldIsToBlame() throws Exception {
        FAILURE.set(new ApiException("VALIDATION_ERROR", "trialDays must be between 0 and 30", "trialDays"));

        mvc.perform(POST_PLANS.build("test-client"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("trialDays must be between 0 and 30"))
                .andExpect(jsonPath("$.field").value("trialDays"));
    }

    @Test
    void S2_errorBodyOmitsFieldWhenNoFieldIsToBlame() throws Exception {
        FAILURE.set(new ApiException("ALREADY_CANCELED", "subscription is canceled", null));

        MvcResult result = mvc.perform(POST_CANCEL.build("test-client"))
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(errorBody(result).has("field")).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointsAndUnknownPath")
    void S2_errorResponsesAreJson(Call call) throws Exception {
        mvc.perform(call.build(null))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ---- 500 INTERNAL_ERROR ------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void S6_unexpectedFailureReturnsGenericInternalError(Call call) throws Exception {
        FAILURE.set(new IllegalStateException("secret-detail: jdbc:h2:mem:subtrack"));

        MvcResult result = mvc.perform(call.build("test-client"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andReturn();

        String raw = result.getResponse().getContentAsString();
        assertThat(raw).doesNotContain("secret-detail", "jdbc", "IllegalStateException", "\tat ", "edu.columbia");
        assertThat(errorBody(result).fieldNames()).toIterable().containsOnly("error", "message");
    }
}
