package com.leadflow;

import com.leadflow.model.Lead;
import com.leadflow.model.LeadStatus;
import com.leadflow.model.Role;
import com.leadflow.model.User;
import com.leadflow.service.LeadFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LeadFactory tier creation and domain entity behavior.
 */
public class LeadFactoryAndModelTest {

    @Test
    void testCreateUrgentLeadSetsHighPriorityAndHalfSla() {
        Lead lead = LeadFactory.createLead(
                "URGENT", "Acme Corp", "acme@example.com", "Server Outage", "Need immediate help", 10);

        assertNotNull(lead.getLeadId());
        assertTrue(lead.getLeadId().startsWith("LD-"));
        assertNotNull(lead.getTrackingUuid());
        assertEquals("URGENT", lead.getSlaTier());
        assertEquals(3, lead.getPriorityLevel());
        assertEquals(LeadStatus.NEW, lead.getStatus());
        assertEquals(1, lead.getMessages().size());
        assertFalse(lead.isSlaExpired());
    }

    @Test
    void testCreateStandardAndEnterpriseTiers() {
        Lead enterprise = LeadFactory.createLead(
                "ENTERPRISE", "Globex", "info@globex.com", "Bulk Quote", "50 seats", 10);
        assertEquals("ENTERPRISE", enterprise.getSlaTier());
        assertEquals(2, enterprise.getPriorityLevel());

        Lead standard = LeadFactory.createLead(
                null, "John Doe", "john@doe.com", "General Question", "Pricing?", 10);
        assertEquals("STANDARD", standard.getSlaTier());
        assertEquals(1, standard.getPriorityLevel());
    }

    @Test
    void testSlaExpirationAndEscalationIncrement() {
        Lead lead = LeadFactory.createLead(
                "STANDARD", "Jane", "jane@test.com", "Follow up", "Hello", 5);
        lead.setSlaDeadline(LocalDateTime.now().minusMinutes(2));

        assertTrue(lead.isSlaExpired());
        assertEquals(1, lead.incrementEscalation());
        assertEquals(2, lead.incrementEscalation());

        lead.setStatus(LeadStatus.CLOSED_WON);
        assertFalse(lead.isSlaExpired(), "Closed leads should never report expired SLA");
    }

    @Test
    void testUserAvailabilityToggle() {
        User u = new User("Test Emp", "emp@test.com", "pass", Role.EMPLOYEE, 1);
        assertTrue(u.isAvailable());
        u.toggleAvailability();
        assertFalse(u.isAvailable());
    }
}