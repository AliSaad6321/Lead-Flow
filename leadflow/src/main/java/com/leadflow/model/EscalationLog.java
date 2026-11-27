package com.leadflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit entity recording multi-tier SLA escalations across employees and owner.
 */
@Entity
@Table(name = "escalation_logs")
public class EscalationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private int logId;

    @Column(name = "lead_id", nullable = false, length = 36)
    private String leadId;

    @Column(name = "from_employee_id", nullable = false)
    private int fromEmployeeId;

    @Column(name = "to_employee_id", nullable = false)
    private int toEmployeeId;

    @Column(name = "escalation_level", nullable = false)
    private int escalationLevel; // 1 = Peer Employee, 2 = Owner

    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    @Column(name = "escalated_at", nullable = false)
    private LocalDateTime escalatedAt;

    /** Default constructor required by Hibernate. */
    public EscalationLog() {}

    /**
     * Constructs an audit record for an SLA escalation event.
     *
     * @param leadId          lead identifier
     * @param fromEmployeeId  previous assignee ID
     * @param toEmployeeId    new assignee ID
     * @param escalationLevel tier (1 = Peer, 2 = Owner)
     * @param reason          description of SLA breach
     */
    public EscalationLog(String leadId, int fromEmployeeId, int toEmployeeId, int escalationLevel, String reason) {
        this.leadId = leadId;
        this.fromEmployeeId = fromEmployeeId;
        this.toEmployeeId = toEmployeeId;
        this.escalationLevel = escalationLevel;
        this.reason = reason;
        this.escalatedAt = LocalDateTime.now();
    }

    public int getLogId() { return logId; }
    public String getLeadId() { return leadId; }
    public int getFromEmployeeId() { return fromEmployeeId; }
    public int getToEmployeeId() { return toEmployeeId; }
    public int getEscalationLevel() { return escalationLevel; }
    public String getReason() { return reason; }
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
}