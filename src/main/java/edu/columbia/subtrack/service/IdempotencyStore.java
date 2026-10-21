package edu.columbia.subtrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.columbia.subtrack.api.ApiException;
import edu.columbia.subtrack.api.ErrorCode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** Remembers the first successful response per client and Idempotency-Key (SPEC.md section 2). */
@Component
public class IdempotencyStore {

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    /**
     * Runs an action at most once per client and key.
     *
     * @param clientKey the calling client
     * @param idempotencyKey the key from the request, or null to run without recording
     * @param body the request body, compared on repeats
     * @param action produces the response on the first request
     * @return the stored response on a repeat, otherwise the action's response
     * @throws ApiException IDEMPOTENCY_MISMATCH if the key was used with a different body
     */
    public StoredResponse execute(final String clientKey, final String idempotencyKey, final JsonNode body,
            final Supplier<StoredResponse> action) {
        if (idempotencyKey == null) {
            return action.get();
        }
        final Entry entry = entries.computeIfAbsent(clientKey + '\n' + idempotencyKey, k -> new Entry());
        synchronized (entry) {
            if (entry.response == null) {
                final StoredResponse response = action.get();
                entry.body = body;
                entry.response = response;
                return response;
            }
            if (!entry.body.equals(body)) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_MISMATCH,
                    "Idempotency-Key was already used with a different request body", null);
            }
            return entry.response;
        }
    }

    /**
     * A response that can be replayed.
     *
     * @param status the HTTP status
     * @param body the response body
     */
    public record StoredResponse(int status, Object body) {
    }

    private static final class Entry {
        private JsonNode body;
        private StoredResponse response;
    }
}
