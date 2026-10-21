package edu.columbia.subtrack.repository;

import edu.columbia.subtrack.domain.Plan;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** In-memory plans, kept per client. */
@Repository
public class PlanRepository {

    private final Map<String, Map<String, Plan>> plansByClientAndName = new HashMap<>();

    /** Stores the plan unless this client already has one with the same name, ignoring case. */
    public synchronized boolean addIfNameFree(String clientKey, Plan plan) {
        String nameKey = plan.terms().name().toLowerCase(Locale.ROOT);
        Map<String, Plan> mine = plansByClientAndName.computeIfAbsent(clientKey, key -> new HashMap<>());
        return mine.putIfAbsent(nameKey, plan) == null;
    }
}
