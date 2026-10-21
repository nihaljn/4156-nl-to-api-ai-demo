package edu.columbia.subtrack.service;

import edu.columbia.subtrack.domain.Plan;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

/** Holds every client's plans in memory. */
@Repository
public class PlanRepository {

    private final ConcurrentMap<String, List<Plan>> plansByClient = new ConcurrentHashMap<>();

    /**
     * Stores a plan unless the client already has one with the same name, ignoring case.
     *
     * @param clientKey the owning client
     * @param plan the plan to store
     * @return true if stored, false if the name was taken
     */
    public boolean addIfNameFree(final String clientKey, final Plan plan) {
        final List<Plan> plans = plansByClient.computeIfAbsent(clientKey, key -> new ArrayList<>());
        final String wanted = plan.name().toLowerCase(Locale.ROOT);
        synchronized (plans) {
            final boolean taken = plans.stream().anyMatch(p -> p.name().toLowerCase(Locale.ROOT).equals(wanted));
            if (!taken) {
                plans.add(plan);
            }
            return !taken;
        }
    }
}
