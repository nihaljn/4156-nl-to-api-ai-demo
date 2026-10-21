package edu.columbia.subtrack.repository;

import edu.columbia.subtrack.domain.Subscription;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Repository;

/** In-memory subscriptions, kept per client. */
@Repository
public class SubscriptionRepository {

    private final Map<String, List<Subscription>> subscriptionsByClient = new HashMap<>();

    /**
     * Stores the subscription unless one of this client's existing subscriptions satisfies {@code blocks};
     * in that case returns the blocking one. Check and store happen together, so two requests can't both pass.
     */
    public synchronized Optional<Subscription> addUnlessBlocked(String clientKey, Subscription subscription,
                                                                Predicate<Subscription> blocks) {
        List<Subscription> mine = subscriptionsByClient.computeIfAbsent(clientKey, key -> new ArrayList<>());
        Optional<Subscription> blocking = mine.stream().filter(blocks).findFirst();
        if (blocking.isEmpty()) {
            mine.add(subscription);
        }
        return blocking;
    }
}
