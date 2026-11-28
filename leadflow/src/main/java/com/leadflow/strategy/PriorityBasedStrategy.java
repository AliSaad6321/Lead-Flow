package com.leadflow.strategy;

import com.leadflow.model.Lead;
import com.leadflow.model.User;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Routes leads to the highest-weighted available employee (breaking ties by lowest workload).
 */
public class PriorityBasedStrategy implements RoutingStrategy {

    @Override
    public Optional<User> assignEmployee(Lead lead, List<User> eligibleEmployees) {
        if (eligibleEmployees == null || eligibleEmployees.isEmpty()) {
            return Optional.empty();
        }
        return eligibleEmployees.stream()
                .max(Comparator.comparingInt(User::getPriorityWeight)
                        .thenComparingInt(u -> -u.getActiveLeadCount()));
    }

    @Override
    public String getStrategyName() {
        return "PRIORITY_BASED";
    }
}