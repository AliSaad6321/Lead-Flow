package com.leadflow.observer;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Observer that records all system lead events to console and an in-memory audit buffer.
 */
public class AuditLogObserver implements LeadEventListener {

    private final List<String> auditEntries = Collections.synchronizedList(new ArrayList<>());
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void onLeadEvent(LeadEvent event) {
        String formatted = String.format("[%s] [%s] Lead %s: %s",
                event.getTimestamp().format(FMT),
                event.getType(),
                event.getLeadId(),
                event.getDescription());
        auditEntries.add(formatted);
        System.out.println(formatted);
    }

    /**
     * Returns an unmodifiable view of recorded audit entries.
     *
     * @return list of audit strings
     */
    public List<String> getAuditEntries() {
        return Collections.unmodifiableList(auditEntries);
    }
}