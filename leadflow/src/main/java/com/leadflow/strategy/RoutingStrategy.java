package com.leadflow.strategy;

import com.leadflow.model.Lead;
import com.leadflow.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Strategy interface defining how incoming or escalated leads are assigned to available employees.
 */
public interface RoutingStrategy {

    /**
     * Selects an employee from the provided list of eligible employees.
     *
     * @param lead               lead being assigned
     * @param eligibleEmployees  available employees eligible for assignment
     * @return Optional containing selected employee, or empty if none available
     */
    Optional<User> assignEmployee(Lead lead, List<User> eligibleEmployees);

    /**
     * Returns the display name of this routing strategy.
     *
     * @return strategy identifier
     */
    String getStrategyName();
}