package edu.columbia.subtrack.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Calls the SubTrack API as one client. */
public final class ApiClient {

    private final MockMvc mvc;
    private final String clientKey;

    /**
     * Creates a client.
     *
     * @param mvc the MockMvc to call through
     * @param clientKey the value sent in X-Client-Key
     */
    public ApiClient(final MockMvc mvc, final String clientKey) {
        this.mvc = mvc;
        this.clientKey = clientKey;
    }

    /**
     * Reads a value from a JSON response body.
     *
     * @param result the response
     * @param path a JsonPath expression
     * @return the value at the path
     * @throws Exception if the body cannot be read
     */
    public static Object json(final MvcResult result, final String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    /**
     * Creates a monthly 999 USD plan and expects 201.
     *
     * @param name the plan name
     * @param trialDays the trial length
     * @return the new plan ID
     * @throws Exception on request failure
     */
    public String createMonthlyPlan(final String name, final int trialDays) throws Exception {
        final String body = "{\"name\":\"" + name + "\",\"priceCents\":999,\"currency\":\"USD\","
            + "\"interval\":\"MONTH\",\"trialDays\":" + trialDays + "}";
        final MvcResult result = send(post("/plans"), body, null);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return (String) json(result, "$.planId");
    }

    /**
     * Subscribes a customer to a plan.
     *
     * @param planId the plan
     * @param customerId the customer
     * @param startDate the start date, or null to omit it
     * @param idempotencyKey the Idempotency-Key, or null to omit it
     * @return the response
     * @throws Exception on request failure
     */
    public MvcResult subscribe(final String planId, final String customerId, final LocalDate startDate,
            final String idempotencyKey) throws Exception {
        final String start = startDate == null ? "" : ",\"startDate\":\"" + startDate + "\"";
        final String body = "{\"planId\":\"" + planId + "\",\"customerId\":\"" + customerId + "\"" + start + "}";
        return send(post("/subscriptions"), body, idempotencyKey);
    }

    /**
     * Subscribes a customer and expects 201.
     *
     * @param planId the plan
     * @param customerId the customer
     * @param startDate the start date, or null to omit it
     * @return the new subscription ID
     * @throws Exception on request failure
     */
    public String subscribeOk(final String planId, final String customerId, final LocalDate startDate)
            throws Exception {
        final MvcResult result = subscribe(planId, customerId, startDate, null);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return (String) json(result, "$.subscriptionId");
    }

    /**
     * Reads a subscription.
     *
     * @param subscriptionId the subscription
     * @param asOf the asOf date, or null to omit it
     * @return the response
     * @throws Exception on request failure
     */
    public MvcResult getSubscription(final String subscriptionId, final LocalDate asOf) throws Exception {
        final String query = asOf == null ? "" : "?asOf=" + asOf;
        return send(get("/subscriptions/" + subscriptionId + query), null, null);
    }

    /**
     * Cancels a subscription.
     *
     * @param subscriptionId the subscription
     * @param mode AT_PERIOD_END or IMMEDIATE
     * @return the response
     * @throws Exception on request failure
     */
    public MvcResult cancel(final String subscriptionId, final String mode) throws Exception {
        return send(post("/subscriptions/" + subscriptionId + "/cancel"), "{\"mode\":\"" + mode + "\"}", null);
    }

    /**
     * Lists billing dates.
     *
     * @param from the first day of the window
     * @param to the last day of the window
     * @return the response
     * @throws Exception on request failure
     */
    public MvcResult billingDates(final LocalDate from, final LocalDate to) throws Exception {
        return send(get("/billing-dates?from=" + from + "&to=" + to), null, null);
    }

    private MvcResult send(final MockHttpServletRequestBuilder request, final String body,
            final String idempotencyKey) throws Exception {
        request.header("X-Client-Key", clientKey).accept(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mvc.perform(request).andReturn();
    }
}
