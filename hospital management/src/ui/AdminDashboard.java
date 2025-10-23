package ui;

import db.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AdminDashboard extends JFrame {
    public AdminDashboard() {
        setTitle("Admin Dashboard");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Add Patient", createAddPatientPanel());

        add(tabbedPane);
    }

    private JPanel createAddPatientPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField nameField = new JTextField();
        JTextField contactField = new JTextField();
        JTextField ageField = new JTextField();
        JTextField genderField = new JTextField();
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Contact:"));
        panel.add(contactField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);
        panel.add(new JLabel("Gender:"));
        panel.add(genderField);
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        JButton addButton = new JButton("Add Patient and Allot Doctor");
        addButton.addActionListener(e -> addPatient(
                nameField.getText(), contactField.getText(), ageField.getText(), genderField.getText(),
                usernameField.getText(), new String(passwordField.getPassword())
        ));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private void addPatient(String name, String contact, String ageStr, String gender, String username, String password) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false); // Start transaction

            // 1. Allot doctor automatically (least busy doctor)
            int doctorId = getLeastBusyDoctorId(conn);
            if (doctorId == -1) {
                JOptionPane.showMessageDialog(this, "No doctors available to assign.", "Error", JOptionPane.ERROR_MESSAGE);
                conn.rollback();
                return;
            }

            // 2. Add patient to users table
            String userSql = "INSERT INTO users (username, password, role) VALUES (?, ?, 'patient')";
            PreparedStatement userPs = conn.prepareStatement(userSql);
            userPs.setString(1, username);
            userPs.setString(2, password);
            userPs.executeUpdate();

            // 3. Add patient to patients table
            String patientSql = "INSERT INTO patients (name, contact, age, gender, username, assigned_doctor_id) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement patientPs = conn.prepareStatement(patientSql);
            patientPs.setString(1, name);
            patientPs.setString(2, contact);
            patientPs.setInt(3, Integer.parseInt(ageStr));
            patientPs.setString(4, gender);
            patientPs.setString(5, username);
            patientPs.setInt(6, doctorId);
            patientPs.executeUpdate();

            conn.commit(); // Commit transaction
            JOptionPane.showMessageDialog(this, "Patient added and assigned to Doctor ID: " + doctorId);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid age.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException closeEx) {
                System.err.println("Failed to reset auto-commit: " + closeEx.getMessage());
            }
        }
    }

    private int getLeastBusyDoctorId(Connection conn) throws SQLException {
        // Simple logic: allot to the doctor with the fewest patients
        String sql = "SELECT d.doctor_id, COUNT(p.patient_id) as patient_count " +
                "FROM doctors d LEFT JOIN patients p ON d.doctor_id = p.assigned_doctor_id " +
                "GROUP BY d.doctor_id ORDER BY patient_count ASC LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("doctor_id");
            }
        }
        return -1; // No doctors found
    }
}