package com.leadflow.strategy;

import com.leadflow.model.Lead;
import com.leadflow.model.User;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Routes leads to the available employee with the fewest currently active leads.
 */
public class LeastLoadedStrategy implements RoutingStrategy {

    @Override
    public Optional<User> assignEmployee(Lead lead, List<User> eligibleEmployees) {
        if (eligibleEmployees == null || eligibleEmployees.isEmpty()) {
            return Optional.empty();
        }
        return eligibleEmployees.stream()
                .min(Comparator.comparingInt(User::getActiveLeadCount)
                        .thenComparingInt(User::getUserId));
    }

    @Override
    public String getStrategyName() {
        return "LEAST_LOADED";
    }
}