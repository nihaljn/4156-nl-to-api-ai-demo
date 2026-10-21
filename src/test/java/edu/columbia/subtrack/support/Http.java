package edu.columbia.subtrack.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Small helpers for driving the API through MockMvc. */
public final class Http {

    public static final String CLIENT_A = "client-a";
    public static final String CLIENT_B = "client-b";

    private Http() {
    }

    public static ResultActions getAs(MockMvc mvc, String client, String path) throws Exception {
        return mvc.perform(get(path).header("X-Client-Key", client));
    }

    public static ResultActions postAs(MockMvc mvc, String client, String path, String body) throws Exception {
        return postWithKey(mvc, client, path, body, null);
    }

    public static ResultActions postWithKey(MockMvc mvc, String client, String path, String body, String idemKey)
            throws Exception {
        MockHttpServletRequestBuilder request = post(path)
                .header("X-Client-Key", client)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (idemKey != null) {
            request.header("Idempotency-Key", idemKey);
        }
        return mvc.perform(request);
    }

    /** Creates a plan named {@code name} (999 USD) and returns its planId. */
    public static String createPlan(MockMvc mvc, String client, String name, String interval, int trialDays)
            throws Exception {
        String body = "{\"name\":\"" + name + "\",\"priceCents\":999,\"currency\":\"USD\",\"interval\":\""
                + interval + "\",\"trialDays\":" + trialDays + "}";
        return read(postAs(mvc, client, "/plans", body).andExpect(status().isCreated()), "$.planId");
    }

    /** Plan M7: MONTH, 999 USD, 7-day trial. */
    public static String createM7(MockMvc mvc, String client) throws Exception {
        return createPlan(mvc, client, "M7", "MONTH", 7);
    }

    public static ResultActions subscribe(MockMvc mvc, String client, String planId, String customerId,
            String startDate) throws Exception {
        String start = startDate == null ? "" : ",\"startDate\":\"" + startDate + "\"";
        String body = "{\"planId\":\"" + planId + "\",\"customerId\":\"" + customerId + "\"" + start + "}";
        return postAs(mvc, client, "/subscriptions", body);
    }

    /** Subscribes and returns the new subscriptionId, expecting 201. */
    public static String subscribeOk(MockMvc mvc, String client, String planId, String customerId,
            String startDate) throws Exception {
        return read(subscribe(mvc, client, planId, customerId, startDate).andExpect(status().isCreated()),
                "$.subscriptionId");
    }

    public static ResultActions cancel(MockMvc mvc, String client, String subscriptionId, String mode)
            throws Exception {
        return postAs(mvc, client, "/subscriptions/" + subscriptionId + "/cancel", "{\"mode\":\"" + mode + "\"}");
    }

    public static String read(ResultActions result, String jsonPath) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, jsonPath);
    }
}
