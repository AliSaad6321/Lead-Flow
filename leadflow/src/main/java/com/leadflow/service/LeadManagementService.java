package com.leadflow.service;

import com.leadflow.dao.HibernateRepository;
import com.leadflow.dao.JdbcTransactionManager;
import com.leadflow.dao.SnapshotIO;
import com.leadflow.model.*;
import com.leadflow.observer.LeadEvent;
import com.leadflow.observer.LeadEventListener;
import com.leadflow.strategy.LeastLoadedStrategy;
import com.leadflow.strategy.PriorityBasedStrategy;
import com.leadflow.strategy.RoundRobinStrategy;
import com.leadflow.strategy.RoutingStrategy;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Core business service orchestrating lead creation, Strategy-based routing,
 * multi-tier SLA escalations, conversation messaging, payments, and state snapshots.
 */
public class LeadManagementService {

    private final HibernateRepository repository;
    private final JdbcTransactionManager jdbcManager;
    private final SnapshotIO snapshotIO;
    private final List<LeadEventListener> listeners = new CopyOnWriteArrayList<>();
    private RoutingStrategy routingStrategy;

    /**
     * Constructs LeadManagementService and initializes the active routing strategy from DB config.
     *
     * @param repository  Hibernate DAO
     * @param jdbcManager JDBC ACID transaction manager
     * @param snapshotIO  Java object serialization handler
     */
    public LeadManagementService(
            HibernateRepository repository,
            JdbcTransactionManager jdbcManager,
            SnapshotIO snapshotIO) {
        this.repository = repository;
        this.jdbcManager = jdbcManager;
        this.snapshotIO = snapshotIO;
        syncStrategyFromConfig();
    }

    /** Registers a real-time event observer. */
    public void addListener(LeadEventListener listener) {
        listeners.add(listener);
    }

    /** Removes a registered event observer. */
    public void removeListener(LeadEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(LeadEvent event) {
        for (LeadEventListener listener : listeners) {
            listener.onLeadEvent(event);
        }
    }

    /** Synchronizes the in-memory RoutingStrategy with persisted SystemConfig. */
    public void syncStrategyFromConfig() {
        SystemConfig cfg = repository.getOrCreateSystemConfig();
        setRoutingStrategyByName(cfg.getRoutingMode());
    }

    /**
     * Switches the active routing algorithm at runtime and persists the choice.
     *
     * @param mode ROUND_ROBIN, PRIORITY_BASED, or LEAST_LOADED
     */
    public void setRoutingStrategyByName(String mode) {
        String normalized = (mode == null) ? "ROUND_ROBIN" : mode.toUpperCase();
        switch (normalized) {
            case "PRIORITY_BASED":
                this.routingStrategy = new PriorityBasedStrategy();
                break;
            case "LEAST_LOADED":
                this.routingStrategy = new LeastLoadedStrategy();
                break;
            case "ROUND_ROBIN":
            default:
                this.routingStrategy = new RoundRobinStrategy();
                normalized = "ROUND_ROBIN";
                break;
        }
        SystemConfig cfg = repository.getOrCreateSystemConfig();
        if (!normalized.equals(cfg.getRoutingMode())) {
            cfg.setRoutingMode(normalized);
            repository.update(cfg);
        }
    }

    /** Updates the organization SLA deadline in minutes. */
    public void updateSlaDeadlineMinutes(int minutes) {
        SystemConfig cfg = repository.getOrCreateSystemConfig();
        cfg.setSlaDeadlineMinutes(Math.max(1, minutes));
        repository.update(cfg);
    }

    public SystemConfig getSystemConfig() {
        return repository.getOrCreateSystemConfig();
    }

    /**
     * Creates a new customer inquiry via LeadFactory, routes it to an available employee
     * using the active RoutingStrategy, and persists it via Hibernate.
     *
     * @param tier            STANDARD, ENTERPRISE, or URGENT
     * @param customerName    guest customer name
     * @param customerContact guest contact details
     * @param subject         inquiry subject
     * @param messageText     initial message body
     * @return created Lead containing its unique tracking UUID
     */
    public Lead submitAndRouteInquiry(
            String tier,
            String customerName,
            String customerContact,
            String subject,
            String messageText) {

        SystemConfig cfg = repository.getOrCreateSystemConfig();
        Lead lead = LeadFactory.createLead(
                tier, customerName, customerContact, subject, messageText, cfg.getSlaDeadlineMinutes());

        List<User> availableEmployees = repository.findAvailableEmployees();
        Optional<User> assignee = routingStrategy.assignEmployee(lead, availableEmployees);

        if (assignee.isEmpty()) {
            // Fallback to Owner if all employees are unavailable
            List<User> owners = repository.findUsersByRole(Role.OWNER);
            if (!owners.isEmpty()) {
                assignee = Optional.of(owners.get(0));
            }
        }

        if (assignee.isPresent()) {
            User chosen = assignee.get();
            lead.setAssignedEmployee(chosen);
            chosen.setActiveLeadCount(chosen.getActiveLeadCount() + 1);
            repository.update(chosen);
        }

        repository.save(lead);

        String assignedName = (lead.getAssignedEmployee() != null)
                ? lead.getAssignedEmployee().getFullName() : "Unassigned Queue";

        notifyListeners(new LeadEvent(
                LeadEvent.EventType.LEAD_CREATED,
                lead.getLeadId(),
                "New " + lead.getSlaTier() + " lead routed to " + assignedName + " (" + routingStrategy.getStrategyName() + ")"
        ));
        return lead;
    }

    /**
     * Appends a follow-up or agent reply message to a lead's preserved conversation thread.
     *
     * @param leadId     target lead ID
     * @param senderRole CUSTOMER, EMPLOYEE, or OWNER
     * @param senderName sender display name
     * @param content    message body
     */
    public void addMessageToLead(String leadId, String senderRole, String senderName, String content) {
        Lead lead = repository.findLeadById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        Message msg = new Message(lead, senderRole, senderName, content);
        lead.getMessages().add(msg);

        // If employee/owner replies to a NEW or ESCALATED lead, transition to IN_PROGRESS
        if (("EMPLOYEE".equals(senderRole) || "OWNER".equals(senderRole))
                && (lead.getStatus() == LeadStatus.NEW || lead.getStatus() == LeadStatus.ESCALATED)) {
            lead.setStatus(LeadStatus.IN_PROGRESS);
        }

        repository.update(lead);
        notifyListeners(new LeadEvent(
                LeadEvent.EventType.MESSAGE_SENT,
                leadId,
                senderName + " (" + senderRole + ") posted a reply."
        ));
    }

    /**
     * Updates a lead's workflow status and adjusts employee workload if closed.
     *
     * @param leadId    target lead ID
     * @param newStatus updated LeadStatus
     */
    public void updateLeadStatus(String leadId, LeadStatus newStatus) {
        Lead lead = repository.findLeadById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        LeadStatus oldStatus = lead.getStatus();
        lead.setStatus(newStatus);
        repository.update(lead);

        if ((newStatus == LeadStatus.CLOSED_WON || newStatus == LeadStatus.CLOSED_LOST)
                && oldStatus != LeadStatus.CLOSED_WON && oldStatus != LeadStatus.CLOSED_LOST
                && lead.getAssignedEmployee() != null) {
            User emp = lead.getAssignedEmployee();
            emp.setActiveLeadCount(Math.max(0, emp.getActiveLeadCount() - 1));
            repository.update(emp);
        }

        notifyListeners(new LeadEvent(
                LeadEvent.EventType.STATUS_UPDATED,
                leadId,
                "Status changed from " + oldStatus + " to " + newStatus
        ));
    }

    /**
     * Records a payment and atomically closes the lead as CLOSED_WON via JDBC transaction.
     *
     * @param leadId        lead ID
     * @param employeeId    recording employee ID
     * @param amount        payment amount
     * @param paymentMethod payment method string
     * @throws SQLException if JDBC transaction fails and rolls back
     */
    public void recordPaymentAndClose(String leadId, int employeeId, double amount, String paymentMethod)
            throws SQLException {
        PaymentTransaction payment = new PaymentTransaction(leadId, employeeId, amount, paymentMethod);
        jdbcManager.recordPaymentAndCloseLeadAtomic(payment);

        notifyListeners(new LeadEvent(
                LeadEvent.EventType.STATUS_UPDATED,
                leadId,
                String.format("Payment of $%.2f (%s) recorded; lead marked CLOSED_WON.", amount, paymentMethod)
        ));
    }

    /**
     * Performs Multi-Tier SLA Escalation on an expired lead inside an atomic JDBC transaction:
     * - Tier 1 (First breach): Reassigns to another available peer Employee.
     * - Tier 2 (Second+ breach or no peers available): Escalates directly to Owner/Admin.
     *
     * @param lead expired Lead entity
     */
    public synchronized void escalateMultiTier(Lead lead) {
        User currentAssignee = lead.getAssignedEmployee();
        int oldEmpId = (currentAssignee != null) ? currentAssignee.getUserId() : 0;
        String oldEmpName = (currentAssignee != null) ? currentAssignee.getFullName() : "Unassigned";

        int nextTier = lead.getEscalationCount() + 1;
        Optional<User> targetUser = Optional.empty();

        if (nextTier == 1) {
            List<User> peerCandidates = repository.findAvailableEmployees().stream()
                    .filter(u -> u.getUserId() != oldEmpId)
                    .collect(Collectors.toList());
            targetUser = routingStrategy.assignEmployee(lead, peerCandidates);
        }

        // Escalate to Owner if Tier >= 2 or no peer employees are available
        if (targetUser.isEmpty()) {
            nextTier = 2;
            List<User> owners = repository.findUsersByRole(Role.OWNER);
            if (!owners.isEmpty()) {
                targetUser = Optional.of(owners.get(0));
            }
        }

        if (targetUser.isEmpty() || targetUser.get().getUserId() == oldEmpId) {
            return; // Already with Owner or no valid escalation target
        }

        User newAssignee = targetUser.get();
        int slaMinutes = repository.getOrCreateSystemConfig().getSlaDeadlineMinutes();
        LocalDateTime newDeadline = LocalDateTime.now().plusMinutes(slaMinutes);

        String reason = (nextTier == 1)
                ? "L1 Peer Escalation: SLA deadline missed by " + oldEmpName
                : "CRITICAL L2 Owner Escalation: Repeated SLA breach (previous: " + oldEmpName + ")";

        String systemNote = "[AUTO-ESCALATION TIER " + nextTier + "] Reassigned from "
                + oldEmpName + " to " + newAssignee.getFullName() + " due to SLA timeout.";

        EscalationLog log = new EscalationLog(lead.getLeadId(), oldEmpId, newAssignee.getUserId(), nextTier, reason);

        try {
            jdbcManager.reassignLeadAtomic(lead.getLeadId(), newAssignee.getUserId(), newDeadline, log, systemNote);
            notifyListeners(new LeadEvent(
                    LeadEvent.EventType.LEAD_ESCALATED,
                    lead.getLeadId(),
                    reason + " -> Assigned to " + newAssignee.getFullName()
            ));
        } catch (SQLException e) {
            System.err.println("JDBC Escalation transaction rolled back for lead " + lead.getLeadId() + ": " + e.getMessage());
        }
    }

    /**
     * Captures current settings and analytics into a serialized binary snapshot file.
     *
     * @param filePath destination .ser path
     * @throws IOException  on serialization error
     * @throws SQLException on revenue query error
     */
    public void exportSystemSnapshot(String filePath) throws IOException, SQLException {
        SystemConfig cfg = repository.getOrCreateSystemConfig();
        List<Lead> allLeads = repository.findAllLeads();

        Map<String, Integer> statusCounts = new HashMap<>();
        for (LeadStatus status : LeadStatus.values()) {
            statusCounts.put(status.name(), 0);
        }
        for (Lead l : allLeads) {
            statusCounts.merge(l.getStatus().name(), 1, Integer::sum);
        }

        double revenue = jdbcManager.calculateTotalRevenue();
        SystemSnapshot snapshot = new SystemSnapshot(
                cfg.getRoutingMode(), cfg.getSlaDeadlineMinutes(), revenue, statusCounts);
        snapshotIO.exportSnapshot(snapshot, filePath);
    }

    /**
     * Restores routing mode and SLA deadline rules from a serialized .ser snapshot file.
     *
     * @param filePath source .ser path
     * @return restored SystemSnapshot for UI summary display
     * @throws IOException            on file read failure
     * @throws ClassNotFoundException on serialVersionUID mismatch
     */
    public SystemSnapshot restoreSystemSnapshot(String filePath) throws IOException, ClassNotFoundException {
        SystemSnapshot snapshot = snapshotIO.importSnapshot(filePath);
        setRoutingStrategyByName(snapshot.getRoutingMode());
        updateSlaDeadlineMinutes(snapshot.getSlaDeadlineMinutes());
        return snapshot;
    }

    public Optional<Lead> findByTrackingUuid(String uuid) {
        return repository.findLeadByTrackingUuid(uuid.trim());
    }

    public List<Lead> getAllLeads() {
        return repository.findAllLeads();
    }

    public List<Lead> getLeadsForUser(int userId) {
        return repository.findLeadsByAssignee(userId);
    }

    public List<EscalationLog> getEscalationLogs() {
        return repository.findAllEscalationLogs();
    }

    public double getTotalRevenue() {
        try {
            return jdbcManager.calculateTotalRevenue();
        } catch (SQLException e) {
            return 0.0;
        }
    }
}