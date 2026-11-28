package com.leadflow.service;

import com.leadflow.dao.HibernateRepository;
import com.leadflow.model.Lead;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background daemon service polling active leads off the Swing EDT
 * and triggering multi-tier escalations whenever SLA deadlines expire.
 */
public class SlaEscalationScheduler {

    private final HibernateRepository repository;
    private final LeadManagementService leadService;
    private final ScheduledExecutorService executor;

    /**
     * Constructs the background SLA monitor.
     *
     * @param repository  Hibernate DAO for querying expired leads
     * @param leadService service handling atomic escalation workflows
     */
    public SlaEscalationScheduler(HibernateRepository repository, LeadManagementService leadService) {
        this.repository = repository;
        this.leadService = leadService;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LeadFlow-SLA-Monitor");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts periodic SLA checks every 15 seconds.
     */
    public void start() {
        executor.scheduleAtFixedRate(this::checkAndEscalateExpiredLeads, 10, 15, TimeUnit.SECONDS);
    }

    /**
     * Queries expired open leads and escalates each one.
     */
    public void checkAndEscalateExpiredLeads() {
        try {
            List<Lead> expiredLeads = repository.findExpiredActiveLeads();
            for (Lead lead : expiredLeads) {
                if (lead.isSlaExpired()) {
                    leadService.escalateMultiTier(lead);
                }
            }
        } catch (Exception e) {
            System.err.println("SLA Scheduler error: " + e.getMessage());
        }
    }

    /**
     * Shuts down the scheduler cleanly on application exit.
     */
    public void stop() {
        executor.shutdownNow();
    }
}