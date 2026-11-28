package com.leadflow.dao;

import com.leadflow.config.DatabaseConfig;
import com.leadflow.model.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object using Hibernate ORM for entity mapping, relationships, and queries.
 */
public class HibernateRepository {

    /**
     * Persists a new entity instance in the database.
     *
     * @param entity entity object to save
     * @param <T>    entity type
     */
    public <T> void save(T entity) {
        Transaction tx = null;
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Hibernate save failed: " + e.getMessage(), e);
        }
    }

    /**
     * Merges changes of a detached entity into the database.
     *
     * @param entity entity object to update
     * @param <T>    entity type
     */
    public <T> void update(T entity) {
        Transaction tx = null;
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Hibernate update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a user by unique email address.
     *
     * @param email user email
     * @return Optional containing User if found
     */
    public Optional<User> findUserByEmail(String email) {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            User user = session.createQuery("FROM User WHERE email = :email", User.class)
                    .setParameter("email", email)
                    .uniqueResult();
            return Optional.ofNullable(user);
        }
    }

    /**
     * Retrieves all users belonging to a specific role.
     *
     * @param role OWNER or EMPLOYEE
     * @return list of matching users
     */
    public List<User> findUsersByRole(Role role) {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            return session.createQuery("FROM User WHERE role = :role ORDER BY userId ASC", User.class)
                    .setParameter("role", role)
                    .list();
        }
    }

    /**
     * Retrieves all employees currently marked as available.
     *
     * @return list of available employees
     */
    public List<User> findAvailableEmployees() {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM User WHERE role = :role AND available = true ORDER BY userId ASC", User.class)
                    .setParameter("role", Role.EMPLOYEE)
                    .list();
        }
    }

    /**
     * Finds a lead by its public guest tracking UUID.
     *
     * @param trackingUuid UUID token given to the customer
     * @return Optional containing Lead with messages if found
     */
    public Optional<Lead> findLeadByTrackingUuid(String trackingUuid) {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            Lead lead = session.createQuery("FROM Lead WHERE trackingUuid = :uuid", Lead.class)
                    .setParameter("uuid", trackingUuid)
                    .uniqueResult();
            return Optional.ofNullable(lead);
        }
    }

    /**
     * Finds a lead by its primary leadId.
     *
     * @param leadId primary key
     * @return Optional containing Lead if found
     */
    public Optional<Lead> findLeadById(String leadId) {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Lead.class, leadId));
        }
    }

    /**
     * Retrieves all leads ordered by creation timestamp descending.
     *
     * @return list of all leads
     */
    public List<Lead> findAllLeads() {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            return session.createQuery("FROM Lead ORDER BY createdAt DESC", Lead.class).list();
        }
    }

    /**
     * Retrieves leads assigned to a specific employee or owner.
     *
     * @param userId assignee user ID
     * @return list of assigned leads
     */
    public List<Lead> findLeadsByAssignee(int userId) {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Lead WHERE assignedEmployee.userId = :uid ORDER BY priorityLevel DESC, createdAt ASC",
                            Lead.class)
                    .setParameter("uid", userId)
                    .list();
        }
    }

    /**
     * Finds all active leads whose SLA deadline has expired.
     *
     * @return list of expired open leads
     */
    public List<Lead> findExpiredActiveLeads() {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Lead WHERE status IN (:s1, :s2, :s3) AND slaDeadline < :now", Lead.class)
                    .setParameter("s1", LeadStatus.NEW)
                    .setParameter("s2", LeadStatus.IN_PROGRESS)
                    .setParameter("s3", LeadStatus.ESCALATED)
                    .setParameter("now", LocalDateTime.now())
                    .list();
        }
    }

    /**
     * Retrieves all escalation audit logs ordered newest first.
     *
     * @return list of escalation logs
     */
    public List<EscalationLog> findAllEscalationLogs() {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            return session.createQuery("FROM EscalationLog ORDER BY escalatedAt DESC", EscalationLog.class).list();
        }
    }

    /**
     * Retrieves or initializes the singleton SystemConfig row.
     *
     * @return active SystemConfig
     */
    public SystemConfig getOrCreateSystemConfig() {
        try (Session session = DatabaseConfig.getSessionFactory().openSession()) {
            SystemConfig cfg = session.get(SystemConfig.class, 1);
            if (cfg == null) {
                cfg = new SystemConfig();
                save(cfg);
            }
            return cfg;
        }
    }
}