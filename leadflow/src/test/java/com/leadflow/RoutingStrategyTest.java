package com.leadflow;

import com.leadflow.model.Lead;
import com.leadflow.model.Role;
import com.leadflow.model.User;
import com.leadflow.strategy.LeastLoadedStrategy;
import com.leadflow.strategy.PriorityBasedStrategy;
import com.leadflow.strategy.RoundRobinStrategy;
import com.leadflow.strategy.RoutingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Strategy Pattern implementations for lead assignment.
 */
public class RoutingStrategyTest {

    private User emp1;
    private User emp2;
    private User emp3;
    private List<User> employees;
    private Lead sampleLead;

    @BeforeEach
    void setUp() {
        emp1 = new User("Hassan", "hassan@leadflow.com", "123", Role.EMPLOYEE, 2);
        emp1.setUserId(1);
        emp1.setActiveLeadCount(4);

        emp2 = new User("Bilal", "bilal@leadflow.com", "123", Role.EMPLOYEE, 5);
        emp2.setUserId(2);
        emp2.setActiveLeadCount(1);

        emp3 = new User("Sara", "sara@leadflow.com", "123", Role.EMPLOYEE, 3);
        emp3.setUserId(3);
        emp3.setActiveLeadCount(2);

        employees = List.of(emp1, emp2, emp3);
        sampleLead = new Lead();
        sampleLead.setLeadId("LD-TEST01");
    }

    @Test
    void testRoundRobinCyclesSequentially() {
        RoutingStrategy rr = new RoundRobinStrategy();
        assertEquals("ROUND_ROBIN", rr.getStrategyName());

        assertEquals(emp1, rr.assignEmployee(sampleLead, employees).orElseThrow());
        assertEquals(emp2, rr.assignEmployee(sampleLead, employees).orElseThrow());
        assertEquals(emp3, rr.assignEmployee(sampleLead, employees).orElseThrow());
        assertEquals(emp1, rr.assignEmployee(sampleLead, employees).orElseThrow());
    }

    @Test
    void testPriorityBasedSelectsHighestWeight() {
        RoutingStrategy priority = new PriorityBasedStrategy();
        assertEquals("PRIORITY_BASED", priority.getStrategyName());

        Optional<User> chosen = priority.assignEmployee(sampleLead, employees);
        assertTrue(chosen.isPresent());
        assertEquals("Bilal", chosen.get().getFullName());
    }

    @Test
    void testLeastLoadedSelectsLowestActiveCount() {
        RoutingStrategy leastLoaded = new LeastLoadedStrategy();
        assertEquals("LEAST_LOADED", leastLoaded.getStrategyName());

        Optional<User> chosen = leastLoaded.assignEmployee(sampleLead, employees);
        assertTrue(chosen.isPresent());
        assertEquals("Bilal", chosen.get().getFullName());
    }

    @Test
    void testEmptyEmployeeListReturnsEmptyOptional() {
        assertTrue(new RoundRobinStrategy().assignEmployee(sampleLead, Collections.emptyList()).isEmpty());
        assertTrue(new PriorityBasedStrategy().assignEmployee(sampleLead, Collections.emptyList()).isEmpty());
        assertTrue(new LeastLoadedStrategy().assignEmployee(sampleLead, Collections.emptyList()).isEmpty());
    }
}