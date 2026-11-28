package com.leadflow.dao;

import com.leadflow.config.DatabaseConfig;
import com.leadflow.model.EscalationLog;
import com.leadflow.model.PaymentTransaction;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Executes multi-step ACID database workflows using raw JDBC parameterized queries,
 * explicit transaction boundaries, commit, and rollback.
 */
public class JdbcTransactionManager {

    private final String url;
    private final String user;
    private final String password;

    /** Default constructor wired to DatabaseConfig credentials. */
    public JdbcTransactionManager() {
        this(DatabaseConfig.JDBC_URL, DatabaseConfig.DB_USER, DatabaseConfig.DB_PASSWORD);
    }

    /**
     * Overloaded constructor allowing custom connection parameters (useful for testing).
     *
     * @param url      JDBC connection string
     * @param user     database username
     * @param password database password
     */
    public JdbcTransactionManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Atomically reassigns an expired lead, inserts an EscalationLog record,
     * appends a SYSTEM audit message, and adjusts active lead counters for both users.
     * Rolls back the entire transaction if any step fails.
     *
     * @param leadId         target lead ID
     * @param newAssigneeId  target employee or owner user ID
     * @param newDeadline    reset SLA deadline
     * @param log            escalation audit metadata
     * @param systemNote     message appended to conversation history
     * @return true if committed successfully
     * @throws SQLException if a database error triggers rollback
     */
    public boolean reassignLeadAtomic(
            String leadId,
            int newAssigneeId,
            LocalDateTime newDeadline,
            EscalationLog log,
            String systemNote) throws SQLException {

        String updateLeadSql = "UPDATE leads SET assigned_employee_id = ?, status = 'ESCALATED', " +
                "escalation_count = escalation_count + 1, sla_deadline = ? WHERE lead_id = ?";
        String insertLogSql = "INSERT INTO escalation_logs (lead_id, from_employee_id, to_employee_id, " +
                "escalation_level, reason, escalated_at) VALUES (?, ?, ?, ?, ?, ?)";
        String insertMsgSql = "INSERT INTO messages (lead_id, sender_role, sender_name, content, sent_at) " +
                "VALUES (?, 'SYSTEM', 'SLA Escalation Engine', ?, ?)";
        String decOldUserSql = "UPDATE users SET active_lead_count = GREATEST(0, active_lead_count - 1) WHERE user_id = ?";
        String incNewUserSql = "UPDATE users SET active_lead_count = active_lead_count + 1 WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psLead = conn.prepareStatement(updateLeadSql);
                 PreparedStatement psLog = conn.prepareStatement(insertLogSql);
                 PreparedStatement psMsg = conn.prepareStatement(insertMsgSql);
                 PreparedStatement psDec = conn.prepareStatement(decOldUserSql);
                 PreparedStatement psInc = conn.prepareStatement(incNewUserSql)) {

                // 1. Update Lead assignee & SLA deadline
                psLead.setInt(1, newAssigneeId);
                psLead.setTimestamp(2, Timestamp.valueOf(newDeadline));
                psLead.setString(3, leadId);
                psLead.executeUpdate();

                // 2. Insert EscalationLog
                psLog.setString(1, leadId);
                psLog.setInt(2, log.getFromEmployeeId());
                psLog.setInt(3, newAssigneeId);
                psLog.setInt(4, log.getEscalationLevel());
                psLog.setString(5, log.getReason());
                psLog.setTimestamp(6, Timestamp.valueOf(log.getEscalatedAt()));
                psLog.executeUpdate();

                // 3. Preserve context with SYSTEM notice in conversation thread
                psMsg.setString(1, leadId);
                psMsg.setString(2, systemNote);
                psMsg.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                psMsg.executeUpdate();

                // 4. Adjust workload counters
                psDec.setInt(1, log.getFromEmployeeId());
                psDec.executeUpdate();

                psInc.setInt(1, newAssigneeId);
                psInc.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Atomically records a payment transaction, marks the lead as CLOSED_WON,
     * and decrements the employee's active lead workload counter.
     *
     * @param payment payment transaction details
     * @return true if committed successfully
     * @throws SQLException if payment insert or lead update fails (triggers rollback)
     */
    public boolean recordPaymentAndCloseLeadAtomic(PaymentTransaction payment) throws SQLException {
        String insertPaymentSql = "INSERT INTO payments (lead_id, recorded_by_employee_id, amount, payment_method, transaction_date) " +
                "VALUES (?, ?, ?, ?, ?)";
        String closeLeadSql = "UPDATE leads SET status = 'CLOSED_WON' WHERE lead_id = ?";
        String decWorkloadSql = "UPDATE users SET active_lead_count = GREATEST(0, active_lead_count - 1) WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psPay = conn.prepareStatement(insertPaymentSql);
                 PreparedStatement psLead = conn.prepareStatement(closeLeadSql);
                 PreparedStatement psUser = conn.prepareStatement(decWorkloadSql)) {

                psPay.setString(1, payment.getLeadId());
                psPay.setInt(2, payment.getRecordedByEmployeeId());
                psPay.setDouble(3, payment.getAmount());
                psPay.setString(4, payment.getPaymentMethod());
                psPay.setTimestamp(5, Timestamp.valueOf(payment.getTransactionDate()));
                psPay.executeUpdate();

                psLead.setString(1, payment.getLeadId());
                psLead.executeUpdate();

                psUser.setInt(1, payment.getRecordedByEmployeeId());
                psUser.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Calculates total revenue collected across all recorded payments via JDBC.
     *
     * @return cumulative revenue sum
     * @throws SQLException on query failure
     */
    public double calculateTotalRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0.0) AS total_rev FROM payments";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total_rev");
            }
            return 0.0;
        }
    }
}