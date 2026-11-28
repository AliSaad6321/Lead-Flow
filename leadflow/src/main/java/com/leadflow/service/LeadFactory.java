package com.leadflow.service;

import com.leadflow.model.Lead;
import com.leadflow.model.LeadStatus;
import com.leadflow.model.Message;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory class responsible for instantiating tiered Lead entities
 * with appropriate priority weights, SLA deadlines, and initial inquiry messages.
 */
public class LeadFactory {

    /**
     * Creates a configured Lead object based on its service tier.
     *
     * @param tier            STANDARD, ENTERPRISE, or URGENT
     * @param customerName    customer display name
     * @param customerContact customer email or phone
     * @param subject         inquiry headline
     * @param initialMessage  initial inquiry details
     * @param baseSlaMinutes  organization base SLA timeout in minutes
     * @return newly instantiated Lead ready for routing
     */
    public static Lead createLead(
            String tier,
            String customerName,
            String customerContact,
            String subject,
            String initialMessage,
            int baseSlaMinutes) {

        Lead lead = new Lead();
        lead.setLeadId("LD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        lead.setTrackingUuid(UUID.randomUUID().toString());
        lead.setCustomerName(customerName);
        lead.setCustomerContact(customerContact);
        lead.setSubject(subject);
        lead.setStatus(LeadStatus.NEW);
        lead.setEscalationCount(0);

        LocalDateTime now = LocalDateTime.now();
        lead.setCreatedAt(now);

        String normalizedTier = (tier == null) ? "STANDARD" : tier.trim().toUpperCase();
        lead.setSlaTier(normalizedTier);

        switch (normalizedTier) {
            case "URGENT":
                lead.setPriorityLevel(3);
                lead.setSlaDeadline(now.plusMinutes(Math.max(1, baseSlaMinutes / 2)));
                break;
            case "ENTERPRISE":
                lead.setPriorityLevel(2);
                lead.setSlaDeadline(now.plusMinutes(Math.max(2, (int) Math.ceil(baseSlaMinutes * 0.75))));
                break;
            case "STANDARD":
            default:
                lead.setSlaTier("STANDARD");
                lead.setPriorityLevel(1);
                lead.setSlaDeadline(now.plusMinutes(Math.max(1, baseSlaMinutes)));
                break;
        }

        Message firstMsg = new Message(lead, "CUSTOMER", customerName, initialMessage);
        lead.getMessages().add(firstMsg);
        return lead;
    }
}