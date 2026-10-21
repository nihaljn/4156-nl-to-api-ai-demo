package edu.columbia.subtrack.service;

import edu.columbia.subtrack.api.ApiException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * SPEC section 2, "Idempotency". The first request with a key runs; repeats with an equal
 * request get its stored result; concurrent repeats wait for it, so nothing is written twice.
 * A request that fails stores nothing, so the key can be used again.
 */
@Service
public class IdempotencyService {

    private record Entry(Object request, CompletableFuture<Object> result) {
    }

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T execute(String clientKey, String key, Object request, Supplier<T> action) {
        if (key == null) {
            return action.get();
        }
        String scopedKey = clientKey + '\n' + key;
        Entry mine = new Entry(request, new CompletableFuture<>());
        Entry first = entries.putIfAbsent(scopedKey, mine);
        if (first == null) {
            return runFirst(scopedKey, mine, action);
        }
        if (!first.request().equals(request)) {
            throw new ApiException("IDEMPOTENCY_MISMATCH",
                    "Idempotency-Key was already used with a different request body", null);
        }
        try {
            return (T) first.result().join();
        } catch (CompletionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private <T> T runFirst(String scopedKey, Entry mine, Supplier<T> action) {
        try {
            T result = action.get();
            mine.result().complete(result);
            return result;
        } catch (RuntimeException e) {
            entries.remove(scopedKey, mine);
            mine.result().completeExceptionally(e);
            throw e;
        }
    }
}
