package com.leadflow.ui;

import com.leadflow.model.Lead;
import com.leadflow.model.Message;
import com.leadflow.service.LeadManagementService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Registration-free Swing portal enabling Customers to submit inquiries,
 * receive a UUID tracking code, view agent responses, and send follow-up messages.
 */
public class CustomerTrackingPortal extends JFrame {

    private final LeadManagementService leadService;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JTextField txtUuidLookup;
    private JLabel lblStatusBadge;
    private JLabel lblAssignedAgent;
    private JTextArea txtChatHistory;
    private JTextField txtFollowUp;
    private Lead currentTrackedLead;

    /**
     * Constructs the Guest Customer Portal window.
     *
     * @param leadService core lead management service
     */
    public CustomerTrackingPortal(LeadManagementService leadService) {
        this.leadService = leadService;
        setTitle("LeadFlow – Customer Inquiry & Tracking Portal (Guest Access)");
        setSize(760, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("1. Submit New Inquiry", buildSubmitPanel());
        tabs.addTab("2. Track Lead & Live Chat (UUID)", buildTrackingPanel());

        add(tabs);
    }

    private JPanel buildSubmitPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel header = new JLabel("Submit a Customer Inquiry (No Account Required)");
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.setForeground(new Color(27, 54, 93));
        panel.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(25);
        JTextField txtContact = new JTextField(25);
        JTextField txtSubject = new JTextField(25);
        JComboBox<String> cmbTier = new JComboBox<>(new String[]{"STANDARD", "ENTERPRISE", "URGENT"});
        JTextArea txtMessage = new JTextArea(5, 25);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);

        int row = 0;
        addFormRow(form, gbc, row++, "Your Full Name:", txtName);
        addFormRow(form, gbc, row++, "Email / Phone:", txtContact);
        addFormRow(form, gbc, row++, "SLA Priority Tier:", cmbTier);
        addFormRow(form, gbc, row++, "Inquiry Subject:", txtSubject);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Inquiry Details:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(txtMessage), gbc);

        panel.add(form, BorderLayout.CENTER);

        JButton btnSubmit = new JButton("Submit Inquiry & Generate Tracking Code");
        btnSubmit.setBackground(new Color(27, 54, 93));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSubmit.setPreferredSize(new Dimension(280, 38));

        btnSubmit.addActionListener(e -> {
            String name = txtName.getText().trim();
            String contact = txtContact.getText().trim();
            String subject = txtSubject.getText().trim();
            String msg = txtMessage.getText().trim();
            String tier = (String) cmbTier.getSelectedItem();

            if (name.isEmpty() || contact.isEmpty() || subject.isEmpty() || msg.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all inquiry fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Lead created = leadService.submitAndRouteInquiry(tier, name, contact, subject, msg);
                txtName.setText("");
                txtContact.setText("");
                txtSubject.setText("");
                txtMessage.setText("");

                // Show copyable UUID dialog and auto-fill tracking tab
                JTextField uuidField = new JTextField(created.getTrackingUuid());
                uuidField.setEditable(false);
                JPanel popup = new JPanel(new BorderLayout(6, 6));
                popup.add(new JLabel("Inquiry submitted! Save your unique Tracking UUID:"), BorderLayout.NORTH);
                popup.add(uuidField, BorderLayout.CENTER);

                JOptionPane.showMessageDialog(this, popup, "Lead Created – " + created.getLeadId(), JOptionPane.INFORMATION_MESSAGE);
                txtUuidLookup.setText(created.getTrackingUuid());
                loadLeadByUuid();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error submitting inquiry: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnSubmit);
        panel.add(south, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildTrackingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top search bar
        JPanel topBar = new JPanel(new BorderLayout(8, 8));
        txtUuidLookup = new JTextField();
        txtUuidLookup.setToolTipText("Paste your UUID tracking code here");
        JButton btnLookup = new JButton("Track Lead");
        btnLookup.addActionListener(e -> loadLeadByUuid());

        topBar.add(new JLabel("Tracking UUID: "), BorderLayout.WEST);
        topBar.add(txtUuidLookup, BorderLayout.CENTER);
        topBar.add(btnLookup, BorderLayout.EAST);

        // Status info strip
        JPanel statusStrip = new JPanel(new GridLayout(1, 2, 10, 0));
        statusStrip.setBorder(new EmptyBorder(8, 0, 4, 0));
        lblStatusBadge = new JLabel("Status: —");
        lblStatusBadge.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblAssignedAgent = new JLabel("Assigned Representative: —");
        lblAssignedAgent.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusStrip.add(lblStatusBadge);
        statusStrip.add(lblAssignedAgent);

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topBar, BorderLayout.NORTH);
        northContainer.add(statusStrip, BorderLayout.SOUTH);
        panel.add(northContainer, BorderLayout.NORTH);

        // Conversation history
        txtChatHistory = new JTextArea();
        txtChatHistory.setEditable(false);
        txtChatHistory.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtChatHistory.setLineWrap(true);
        txtChatHistory.setWrapStyleWord(true);
        panel.add(new JScrollPane(txtChatHistory), BorderLayout.CENTER);

        // Bottom reply bar
        JPanel replyBar = new JPanel(new BorderLayout(8, 8));
        txtFollowUp = new JTextField();
        JButton btnSend = new JButton("Send Follow-Up");
        btnSend.addActionListener(e -> sendCustomerFollowUp());

        replyBar.add(txtFollowUp, BorderLayout.CENTER);
        replyBar.add(btnSend, BorderLayout.EAST);
        panel.add(replyBar, BorderLayout.SOUTH);

        return panel;
    }

    private void loadLeadByUuid() {
        String uuid = txtUuidLookup.getText().trim();
        if (uuid.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a Tracking UUID first.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Optional<Lead> found = leadService.findByTrackingUuid(uuid);
        if (found.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No inquiry found for UUID: " + uuid, "Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }
        currentTrackedLead = found.get();
        lblStatusBadge.setText("Status: " + currentTrackedLead.getStatus() + " [" + currentTrackedLead.getSlaTier() + "]");
        String agent = (currentTrackedLead.getAssignedEmployee() != null)
                ? currentTrackedLead.getAssignedEmployee().getFullName() : "Pending Assignment";
        lblAssignedAgent.setText("Assigned Representative: " + agent);

        StringBuilder sb = new StringBuilder();
        sb.append("Subject: ").append(currentTrackedLead.getSubject()).append("\n");
        sb.append("------------------------------------------------------------\n");
        for (Message m : currentTrackedLead.getMessages()) {
            sb.append(String.format("[%s] %s (%s):\n%s\n\n",
                    m.getSentAt().format(FMT), m.getSenderName(), m.getSenderRole(), m.getContent()));
        }
        txtChatHistory.setText(sb.toString());
    }

    private void sendCustomerFollowUp() {
        if (currentTrackedLead == null) {
            JOptionPane.showMessageDialog(this, "Look up a valid Tracking UUID before sending a message.", "No Lead Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String text = txtFollowUp.getText().trim();
        if (text.isEmpty()) return;

        leadService.addMessageToLead(
                currentTrackedLead.getLeadId(),
                "CUSTOMER",
                currentTrackedLead.getCustomerName(),
                text
        );
        txtFollowUp.setText("");
        loadLeadByUuid();
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }
}