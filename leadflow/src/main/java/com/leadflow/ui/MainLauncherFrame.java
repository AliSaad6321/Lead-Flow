package com.leadflow.ui;

import com.leadflow.model.Role;
import com.leadflow.model.User;
import com.leadflow.service.AuthService;
import com.leadflow.service.LeadManagementService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

/**
 * Main entry window providing direct guest access to the Customer Portal
 * and role-based login for Employees and Owner/Admin.
 */
public class MainLauncherFrame extends JFrame {

    private final AuthService authService;
    private final LeadManagementService leadService;

    private JTextField txtEmail;
    private JPasswordField txtPassword;

    /**
     * Constructs the LeadFlow launcher window.
     *
     * @param authService authentication service
     * @param leadService core lead management service
     */
    public MainLauncherFrame(AuthService authService, LeadManagementService leadService) {
        this.authService = authService;
        this.leadService = leadService;

        setTitle("LeadFlow – Centralized Lead Management & SLA Escalation");
        setSize(520, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(20, 26, 20, 26));

        // Title Banner
        JPanel header = new JPanel(new GridLayout(2, 1));
        JLabel title = new JLabel("LeadFlow", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(new Color(27, 54, 93));

        JLabel subtitle = new JLabel("Automated Lead Routing & Multi-Tier SLA Escalation System", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Color.DARK_GRAY);

        header.add(title);
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        // Center Login Card
        JPanel loginCard = new JPanel(new GridBagLayout());
        loginCard.setBorder(BorderFactory.createTitledBorder("Staff Login (Owner / Employee)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtEmail = new JTextField(20);
        txtPassword = new JPasswordField(20);

        gbc.gridx = 0; gbc.gridy = 0;
        loginCard.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        loginCard.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        loginCard.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        loginCard.add(txtPassword, gbc);

        JButton btnLogin = new JButton("Sign In to Dashboard");
        btnLogin.setBackground(new Color(27, 54, 93));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLogin.addActionListener(e -> performLogin());

        gbc.gridx = 1; gbc.gridy = 2;
        loginCard.add(btnLogin, gbc);

        // Quick Demo Autofill Bar
        JPanel demoBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JButton btnFillOwner = new JButton("Demo Owner");
        JButton btnFillEmp1 = new JButton("Demo Employee 1");
        JButton btnFillEmp2 = new JButton("Demo Employee 2");

        btnFillOwner.addActionListener(e -> { txtEmail.setText("owner@leadflow.com"); txtPassword.setText("admin123"); });
        btnFillEmp1.addActionListener(e -> { txtEmail.setText("hassan@leadflow.com"); txtPassword.setText("emp123"); });
        btnFillEmp2.addActionListener(e -> { txtEmail.setText("bilal@leadflow.com"); txtPassword.setText("emp123"); });

        demoBar.add(btnFillOwner);
        demoBar.add(btnFillEmp1);
        demoBar.add(btnFillEmp2);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        loginCard.add(demoBar, gbc);

        root.add(loginCard, BorderLayout.CENTER);

        // Bottom Guest Customer Portal Button
        JPanel guestPanel = new JPanel(new BorderLayout());
        guestPanel.setBorder(BorderFactory.createTitledBorder("Customer Self-Service (No Account Required)"));
        JButton btnGuestPortal = new JButton("Open Customer Inquiry & Tracking Portal");
        btnGuestPortal.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnGuestPortal.setPreferredSize(new Dimension(300, 38));
        btnGuestPortal.addActionListener(e -> new CustomerTrackingPortal(leadService).setVisible(true));
        guestPanel.add(btnGuestPortal, BorderLayout.CENTER);

        root.add(guestPanel, BorderLayout.SOUTH);
        add(root);
    }

    private void performLogin() {
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword());

        Optional<User> userOpt = authService.authenticate(email, pass);
        if (userOpt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid email or password.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = userOpt.get();
        if (user.getRole() == Role.OWNER) {
            new OwnerDashboardFrame(user, leadService, authService).setVisible(true);
        } else {
            new EmployeeWorkspaceFrame(user, leadService, authService).setVisible(true);
        }
    }
}