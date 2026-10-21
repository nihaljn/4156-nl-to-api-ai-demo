package edu.columbia.subtrack.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Thin JSON client over MockMvc so tests read like the SPEC's Given/When/Then tables. */
public final class SubTrackApi {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final MockMvc mvc;

    public SubTrackApi(MockMvc mvc) {
        this.mvc = mvc;
    }

    /** One HTTP response with its parsed body. */
    public record Response(int status, String raw, JsonNode body) {

        public String text(String field) {
            JsonNode node = body.get(field);
            return node == null || node.isNull() ? null : node.asText();
        }

        public boolean has(String field) {
            return body.has(field);
        }

        public boolean isNull(String field) {
            return body.has(field) && body.get(field).isNull();
        }

        public boolean flag(String field) {
            return body.get(field).asBoolean();
        }

        public Response expectStatus(int expected) {
            assertThat(status).as("status; body was %s", raw).isEqualTo(expected);
            return this;
        }

        public Response expectError(int expectedStatus, String code) {
            expectStatus(expectedStatus);
            assertThat(text("error")).as("error code; body was %s", raw).isEqualTo(code);
            assertThat(text("message")).as("message; body was %s", raw).isNotBlank();
            return this;
        }

        public Response expectValidationError(String field) {
            expectError(400, "VALIDATION_ERROR");
            assertThat(text("field")).as("field; body was %s", raw).isEqualTo(field);
            return this;
        }

        public List<String> chargeDates() {
            List<String> dates = new ArrayList<>();
            body.get("charges").forEach(charge -> dates.add(charge.get("date").asText()));
            return dates;
        }
    }

    /** Builds a JSON object body from key/value pairs; a null value leaves the key out. */
    public static Map<String, Object> json(Object... keyValues) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                body.put((String) keyValues[i], keyValues[i + 1]);
            }
        }
        return body;
    }

    public static Map<String, Object> planBody(String name, long priceCents, String currency,
                                               String interval, int trialDays) {
        return json("name", name, "priceCents", priceCents, "currency", currency,
                "interval", interval, "trialDays", trialDays);
    }

    public Response send(String method, String client, String uri, Object body, String idempotencyKey) {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .request(HttpMethod.valueOf(method), uri)
                .accept(MediaType.APPLICATION_JSON);
        if (client != null) {
            request.header("X-Client-Key", client);
        }
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(serialize(body));
        }
        try {
            MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();
            String raw = response.getContentAsString(StandardCharsets.UTF_8);
            return new Response(response.getStatus(), raw, parse(raw));
        } catch (Exception e) {
            throw new IllegalStateException("request failed: " + method + " " + uri, e);
        }
    }

    private static String serialize(Object body) {
        if (body instanceof String text) {
            return text;
        }
        try {
            return JSON.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static JsonNode parse(String raw) {
        if (raw.isBlank()) {
            return JSON.missingNode();
        }
        try {
            return JSON.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new AssertionError("response body is not JSON: " + raw, e);
        }
    }

    // ---- plans -------------------------------------------------------------------------------

    public Response postPlan(String client, Object body, String idempotencyKey) {
        return send("POST", client, "/plans", body, idempotencyKey);
    }

    public String createPlan(String client, String name, String interval, long priceCents, int trialDays) {
        return createPlan(client, name, interval, priceCents, "USD", trialDays);
    }

    public String createPlan(String client, String name, String interval, long priceCents,
                             String currency, int trialDays) {
        return postPlan(client, planBody(name, priceCents, currency, interval, trialDays), null)
                .expectStatus(201).text("planId");
    }

    // ---- subscriptions -----------------------------------------------------------------------

    public Response postSubscription(String client, Object body, String idempotencyKey) {
        return send("POST", client, "/subscriptions", body, idempotencyKey);
    }

    public Response subscribe(String client, String planId, String customerId, String startDate) {
        return subscribe(client, planId, customerId, startDate, null);
    }

    public Response subscribe(String client, String planId, String customerId, String startDate,
                              String idempotencyKey) {
        return postSubscription(client,
                json("planId", planId, "customerId", customerId, "startDate", startDate), idempotencyKey);
    }

    /** Subscribes, requires 201, and returns the new subscriptionId. */
    public String subscribeOk(String client, String planId, String customerId, String startDate) {
        return subscribe(client, planId, customerId, startDate).expectStatus(201).text("subscriptionId");
    }

    public Response view(String client, String subscriptionId, String asOf) {
        String query = asOf == null ? "" : "?asOf=" + asOf;
        return send("GET", client, "/subscriptions/" + subscriptionId + query, null, null);
    }

    public Response cancel(String client, String subscriptionId, String mode) {
        return send("POST", client, "/subscriptions/" + subscriptionId + "/cancel",
                json("mode", mode), null);
    }

    public Response billingDates(String client, String from, String to, String customerId) {
        StringBuilder uri = new StringBuilder("/billing-dates?");
        if (from != null) {
            uri.append("from=").append(from).append('&');
        }
        if (to != null) {
            uri.append("to=").append(to).append('&');
        }
        if (customerId != null) {
            uri.append("customerId=").append(customerId).append('&');
        }
        return send("GET", client, uri.toString(), null, null);
    }
}
