package com.leadflow;

import com.leadflow.observer.AuditLogObserver;
import com.leadflow.observer.LeadEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Observer Pattern event logging.
 */
public class ObserverPatternTest {

    @Test
    void testAuditLogObserverCapturesPublishedEvents() {
        AuditLogObserver observer = new AuditLogObserver();
        LeadEvent event = new LeadEvent(
                LeadEvent.EventType.LEAD_ESCALATED,
                "LD-9999",
                "L1 Peer Escalation triggered"
        );

        observer.onLeadEvent(event);

        assertEquals(1, observer.getAuditEntries().size());
        String logLine = observer.getAuditEntries().get(0);
        assertTrue(logLine.contains("LEAD_ESCALATED"));
        assertTrue(logLine.contains("LD-9999"));
        assertTrue(logLine.contains("L1 Peer Escalation triggered"));
    }
}