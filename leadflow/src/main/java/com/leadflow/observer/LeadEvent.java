package com.leadflow.observer;

import java.time.LocalDateTime;

/**
 * Immutable event payload broadcast when lead assignments, messages, or escalations occur.
 */
public class LeadEvent {

    /** Categorizes system domain events. */
    public enum EventType {
        LEAD_CREATED,
        LEAD_ASSIGNED,
        MESSAGE_SENT,
        STATUS_UPDATED,
        LEAD_ESCALATED
    }

    private final EventType type;
    private final String leadId;
    private final String description;
    private final LocalDateTime timestamp;

    /**
     * Constructs a new LeadEvent.
     *
     * @param type        category of event
     * @param leadId      affected lead identifier
     * @param description human-readable event summary
     */
    public LeadEvent(EventType type, String leadId, String description) {
        this.type = type;
        this.leadId = leadId;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public EventType getType() { return type; }
    public String getLeadId() { return leadId; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
}