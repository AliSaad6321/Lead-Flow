package com.leadflow.ui;

import com.leadflow.model.Lead;
import com.leadflow.model.LeadStatus;
import com.leadflow.model.Message;
import com.leadflow.model.User;
import com.leadflow.observer.SwingDashboardObserver;
import com.leadflow.service.AuthService;
import com.leadflow.service.LeadManagementService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Employee Workspace Frame synchronized in real time via SwingDashboardObserver.
 * Enables employees to respond to assigned leads, transition statuses, and log payments.
 */
public class EmployeeWorkspaceFrame extends JFrame {

    private final User currentUser;
    private final LeadManagementService leadService;
    private final AuthService authService;
    private final SwingDashboardObserver uiObserver;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

    private DefaultTableModel tableModel;
    private JTable leadsTable;
    private JTextArea txtConversation;
    private JTextField txtReply;
    private JToggleButton btnAvailability;
    private List<Lead> cachedLeads;

    /**
     * Constructs the workspace window for a logged-in Employee.
     *
     * @param currentUser logged-in Employee entity
     * @param leadService core lead service
     * @param authService authentication/user service
     */
    public EmployeeWorkspaceFrame(User currentUser, LeadManagementService leadService, AuthService authService) {
        this.currentUser = currentUser;
        this.leadService = leadService;
        this.authService = authService;

        setTitle("LeadFlow – Employee Workspace | " + currentUser.getFullName());
        setSize(980, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initLayout();

        // Register Observer for real-time dashboard refresh
        this.uiObserver = new SwingDashboardObserver(this, this::refreshLeadsTable);
        this.leadService.addListener(uiObserver);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                leadService.removeListener(uiObserver);
            }
        });

        refreshLeadsTable();
    }

    private void initLayout() {
        setLayout(new BorderLayout(10, 10));

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(new EmptyBorder(10, 15, 10, 15));
        topBar.setBackground(new Color(27, 54, 93));

        JLabel lblWelcome = new JLabel("Agent Workspace: " + currentUser.getFullName() + " (" + currentUser.getEmail() + ")");
        lblWelcome.setForeground(Color.WHITE);
        lblWelcome.setFont(new Font("SansSerif", Font.BOLD, 14));
        topBar.add(lblWelcome, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);

        btnAvailability = new JToggleButton(
                currentUser.isAvailable() ? "Status: AVAILABLE" : "Status: UNAVAILABLE",
                currentUser.isAvailable()
        );
        btnAvailability.addActionListener(e -> {
            authService.toggleEmployeeAvailability(currentUser);
            btnAvailability.setText(currentUser.isAvailable() ? "Status: AVAILABLE" : "Status: UNAVAILABLE");
        });

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> refreshLeadsTable());

        rightControls.add(btnAvailability);
        rightControls.add(btnRefresh);
        topBar.add(rightControls, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Split Pane: Left Table, Right Conversation & Actions
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(510);
        splitPane.setBorder(new EmptyBorder(0, 10, 10, 10));

        // Left: Assigned Leads Table
        String[] cols = {"Lead ID", "Customer", "Subject", "Tier", "Status", "SLA Deadline"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        leadsTable = new JTable(tableModel);
        leadsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leadsTable.setRowHeight(24);
        leadsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedLeadDetails();
            }
        });

        splitPane.setLeftComponent(new JScrollPane(leadsTable));

        // Right: Conversation History + Reply + Status/Payment Controls
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        txtConversation = new JTextArea();
        txtConversation.setEditable(false);
        txtConversation.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtConversation.setLineWrap(true);
        txtConversation.setWrapStyleWord(true);
        rightPanel.add(new JScrollPane(txtConversation), BorderLayout.CENTER);

        JPanel replyPanel = new JPanel(new BorderLayout(6, 6));
        txtReply = new JTextField();
        JButton btnSendReply = new JButton("Send Reply");
        btnSendReply.addActionListener(e -> handleSendReply());
        replyPanel.add(txtReply, BorderLayout.CENTER);
        replyPanel.add(btnSendReply, BorderLayout.EAST);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnMarkInProgress = new JButton("Accept / In Progress");
        JButton btnRecordPayment = new JButton("Record Payment & Close Won");
        JButton btnCloseLost = new JButton("Mark Closed-Lost");

        btnRecordPayment.setBackground(new Color(40, 120, 70));
        btnRecordPayment.setForeground(Color.WHITE);

        btnMarkInProgress.addActionListener(e -> updateSelectedStatus(LeadStatus.IN_PROGRESS));
        btnCloseLost.addActionListener(e -> updateSelectedStatus(LeadStatus.CLOSED_LOST));
        btnRecordPayment.addActionListener(e -> openPaymentModal());

        actionBar.add(btnMarkInProgress);
        actionBar.add(btnRecordPayment);
        actionBar.add(btnCloseLost);

        JPanel southBox = new JPanel(new BorderLayout(6, 6));
        southBox.add(replyPanel, BorderLayout.NORTH);
        southBox.add(actionBar, BorderLayout.SOUTH);

        rightPanel.add(southBox, BorderLayout.SOUTH);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void refreshLeadsTable() {
        int selectedRow = leadsTable.getSelectedRow();
        String selectedLeadId = (selectedRow >= 0 && cachedLeads != null && selectedRow < cachedLeads.size())
                ? cachedLeads.get(selectedRow).getLeadId() : null;

        cachedLeads = leadService.getLeadsForUser(currentUser.getUserId());
        tableModel.setRowCount(0);

        int restoreIndex = -1;
        for (int i = 0; i < cachedLeads.size(); i++) {
            Lead l = cachedLeads.get(i);
            tableModel.addRow(new Object[]{
                    l.getLeadId(),
                    l.getCustomerName(),
                    l.getSubject(),
                    l.getSlaTier(),
                    l.getStatus(),
                    l.getSlaDeadline().format(FMT)
            });
            if (l.getLeadId().equals(selectedLeadId)) {
                restoreIndex = i;
            }
        }

        if (restoreIndex >= 0) {
            leadsTable.setRowSelectionInterval(restoreIndex, restoreIndex);
            displaySelectedLeadDetails();
        }
    }

    private Lead getSelectedLead() {
        int row = leadsTable.getSelectedRow();
        if (row < 0 || cachedLeads == null || row >= cachedLeads.size()) {
            return null;
        }
        return cachedLeads.get(row);
    }

    private void displaySelectedLeadDetails() {
        Lead lead = getSelectedLead();
        if (lead == null) {
            txtConversation.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Lead ID: ").append(lead.getLeadId())
          .append(" | Customer: ").append(lead.getCustomerName())
          .append(" (").append(lead.getCustomerContact()).append(")\n");
        sb.append("Status: ").append(lead.getStatus())
          .append(" | Escalations: ").append(lead.getEscalationCount()).append("\n");
        sb.append("============================================================\n");
        for (Message m : lead.getMessages()) {
            sb.append(String.format("[%s] %s (%s):\n%s\n\n",
                    m.getSentAt().format(FMT), m.getSenderName(), m.getSenderRole(), m.getContent()));
        }
        txtConversation.setText(sb.toString());
    }

    private void handleSendReply() {
        Lead lead = getSelectedLead();
        if (lead == null) {
            JOptionPane.showMessageDialog(this, "Select a lead first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String text = txtReply.getText().trim();
        if (text.isEmpty()) return;

        leadService.addMessageToLead(lead.getLeadId(), "EMPLOYEE", currentUser.getFullName(), text);
        txtReply.setText("");
        refreshLeadsTable();
    }

    private void updateSelectedStatus(LeadStatus newStatus) {
        Lead lead = getSelectedLead();
        if (lead == null) {
            JOptionPane.showMessageDialog(this, "Select a lead first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        leadService.updateLeadStatus(lead.getLeadId(), newStatus);
        refreshLeadsTable();
    }

    private void openPaymentModal() {
        Lead lead = getSelectedLead();
        if (lead == null) {
            JOptionPane.showMessageDialog(this, "Select a lead first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (lead.getStatus() == LeadStatus.CLOSED_WON) {
            JOptionPane.showMessageDialog(this, "This lead is already Closed-Won.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JTextField txtAmount = new JTextField("150.00");
        JComboBox<String> cmbMethod = new JComboBox<>(new String[]{"Credit Card", "Bank Transfer", "Cash", "Online Wallet"});

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Payment Amount ($):"));
        form.add(txtAmount);
        form.add(new JLabel("Payment Method:"));
        form.add(cmbMethod);

        int res = JOptionPane.showConfirmDialog(this, form, "Log Payment & Close Lead (" + lead.getLeadId() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res == JOptionPane.OK_OPTION) {
            try {
                double amount = Double.parseDouble(txtAmount.getText().trim());
                if (amount <= 0) throw new NumberFormatException();
                String method = (String) cmbMethod.getSelectedItem();

                leadService.recordPaymentAndClose(lead.getLeadId(), currentUser.getUserId(), amount, method);
                JOptionPane.showMessageDialog(this, "Payment committed via JDBC transaction and lead marked CLOSED_WON!");
                refreshLeadsTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid positive payment amount.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Transaction failed & rolled back: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}