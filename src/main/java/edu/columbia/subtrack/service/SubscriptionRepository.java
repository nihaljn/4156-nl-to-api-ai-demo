package edu.columbia.subtrack.service;

import edu.columbia.subtrack.domain.Subscription;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import org.springframework.stereotype.Repository;

/** Holds every client's subscriptions in memory. */
@Repository
public class SubscriptionRepository {

    private final ConcurrentMap<String, List<Subscription>> subscriptionsByClient = new ConcurrentHashMap<>();

    /**
     * Stores a subscription unless an existing one of the same client blocks it.
     *
     * @param clientKey the owning client
     * @param candidate the subscription to store
     * @param blocks tells whether an existing subscription blocks the candidate
     * @return the blocking subscription, or empty if the candidate was stored
     */
    public Optional<Subscription> addUnlessBlocked(final String clientKey, final Subscription candidate,
            final Predicate<Subscription> blocks) {
        final List<Subscription> subscriptions =
            subscriptionsByClient.computeIfAbsent(clientKey, key -> new ArrayList<>());
        synchronized (subscriptions) {
            final Optional<Subscription> blocking = subscriptions.stream().filter(blocks).findFirst();
            if (blocking.isEmpty()) {
                subscriptions.add(candidate);
            }
            return blocking;
        }
    }
}
