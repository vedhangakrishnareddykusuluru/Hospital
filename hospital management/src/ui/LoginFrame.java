package ui;

import db.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField userText;
    private JPasswordField passText;

    public LoginFrame() {
        setTitle("Hospital Management System - Login");
        setSize(400, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Username:"));
        userText = new JTextField();
        panel.add(userText);

        panel.add(new JLabel("Password:"));
        passText = new JPasswordField();
        panel.add(passText);

        JButton loginButton = new JButton("Login");
        panel.add(new JLabel()); // Placeholder
        panel.add(loginButton);

        add(panel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> performLogin());
    }

    private void performLogin() {
        String username = userText.getText();
        String password = new String(passText.getPassword());

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT role, password FROM users WHERE username = ?")) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                if (password.equals(dbPassword)) {
                    String role = rs.getString("role");
                    JOptionPane.showMessageDialog(this, "Login Successful!");
                    this.dispose(); // Close login window

                    // Open the respective dashboard
                    switch (role) {
                        case "admin":
                            new AdminDashboard().setVisible(true);
                            break;
                        case "doctor":
                            new DoctorDashboard(username).setVisible(true);
                            break;
                        case "patient":
                            new PatientDashboard(username).setVisible(true);
                            break;
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Password!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Username not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}