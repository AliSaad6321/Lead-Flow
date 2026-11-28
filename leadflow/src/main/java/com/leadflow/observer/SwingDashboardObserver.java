package com.leadflow.observer;

import javax.swing.*;

/**
 * Thread-safe UI Observer that marshals real-time refresh callbacks onto the Swing EDT.
 */
public class SwingDashboardObserver implements LeadEventListener {

    private final Runnable refreshCallback;
    private final JFrame parentFrame;

    /**
     * Constructs a UI observer bound to a Swing frame and refresh action.
     *
     * @param parentFrame     owning Swing frame for pop-up escalation alerts (nullable)
     * @param refreshCallback UI reload action executed on the EDT
     */
    public SwingDashboardObserver(JFrame parentFrame, Runnable refreshCallback) {
        this.parentFrame = parentFrame;
        this.refreshCallback = refreshCallback;
    }

    @Override
    public void onLeadEvent(LeadEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (refreshCallback != null) {
                refreshCallback.run();
            }
            if (parentFrame != null && parentFrame.isVisible()
                    && event.getType() == LeadEvent.EventType.LEAD_ESCALATED) {
                JOptionPane.showMessageDialog(
                        parentFrame,
                        event.getDescription(),
                        "SLA Escalation Alert",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });
    }
}