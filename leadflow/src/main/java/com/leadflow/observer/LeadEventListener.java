package com.leadflow.observer;

/**
 * Observer interface implemented by components reacting to real-time LeadFlow events.
 */
public interface LeadEventListener {

    /**
     * Invoked whenever a domain event is published by LeadManagementService.
     *
     * @param event event details
     */
    void onLeadEvent(LeadEvent event);
}