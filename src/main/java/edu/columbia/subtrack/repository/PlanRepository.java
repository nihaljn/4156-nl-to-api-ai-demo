package edu.columbia.subtrack.repository;

import edu.columbia.subtrack.domain.Plan;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** In-memory plans, kept per client. */
@Repository
public class PlanRepository {

    private final Map<String, Map<String, Plan>> plansByClientAndName = new HashMap<>();
    private final Map<String, Map<String, Plan>> plansByClientAndId = new HashMap<>();

    /** Stores the plan unless this client already has one with the same name, ignoring case. */
    public synchronized boolean addIfNameFree(String clientKey, Plan plan) {
        String nameKey = plan.terms().name().toLowerCase(Locale.ROOT);
        Map<String, Plan> mine = plansByClientAndName.computeIfAbsent(clientKey, key -> new HashMap<>());
        if (mine.putIfAbsent(nameKey, plan) != null) {
            return false;
        }
        plansByClientAndId.computeIfAbsent(clientKey, key -> new HashMap<>()).put(plan.planId(), plan);
        return true;
    }

    public synchronized Optional<Plan> findById(String clientKey, String planId) {
        return Optional.ofNullable(plansByClientAndId.getOrDefault(clientKey, Map.of()).get(planId));
    }
}
