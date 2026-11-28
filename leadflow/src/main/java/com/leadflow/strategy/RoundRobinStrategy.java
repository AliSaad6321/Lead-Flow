package com.leadflow.strategy;

import com.leadflow.model.Lead;
import com.leadflow.model.User;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Distributes leads sequentially across available employees in circular order.
 */
public class RoundRobinStrategy implements RoutingStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Optional<User> assignEmployee(Lead lead, List<User> eligibleEmployees) {
        if (eligibleEmployees == null || eligibleEmployees.isEmpty()) {
            return Optional.empty();
        }
        int index = Math.abs(counter.getAndIncrement() % eligibleEmployees.size());
        return Optional.of(eligibleEmployees.get(index));
    }

    @Override
    public String getStrategyName() {
        return "ROUND_ROBIN";
    }
}