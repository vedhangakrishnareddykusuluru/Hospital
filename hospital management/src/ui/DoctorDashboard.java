package ui;

import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DoctorDashboard extends JFrame {
    private String doctorUsername;
    private int doctorId;

    public DoctorDashboard(String username) {
        this.doctorUsername = username;
        this.doctorId = getDoctorIdFromUsername(username);

        setTitle("Doctor Dashboard - Welcome " + username);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("My Appointments", createAppointmentsPanel());
        tabbedPane.add("Add Medical Record / Bill", createAddRecordPanel());

        add(tabbedPane);
    }

    private int getDoctorIdFromUsername(String username) {
        String sql = "SELECT doctor_id FROM doctors WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("doctor_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private JPanel createAppointmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        String sql = "SELECT a.appointment_date, p.name AS patient_name, a.status FROM appointments a " +
                "JOIN patients p ON a.patient_id = p.patient_id WHERE a.doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                model.addColumn(metaData.getColumnLabel(i));
            }
            while (rs.next()) {
                Object[] row = new Object[metaData.getColumnCount()];
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    row[i - 1] = rs.getObject(i);
                }
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return panel;
    }

    private JPanel createAddRecordPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form Panel
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 5, 5));
        JComboBox<String> patientComboBox = new JComboBox<>();
        JTextField visitDateField = new JTextField(); // YYYY-MM-DD
        JTextArea diagnosisArea = new JTextArea(3, 20);
        JTextArea prescriptionArea = new JTextArea(3, 20);
        JTextField billAmountField = new JTextField();
        JTextField billDetailsField = new JTextField();

        // Populate patients assigned to this doctor
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT patient_id, name FROM patients WHERE assigned_doctor_id = ?")) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                patientComboBox.addItem(rs.getInt("patient_id") + ": " + rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        formPanel.add(new JLabel("Select Patient:"));
        formPanel.add(patientComboBox);
        formPanel.add(new JLabel("Visit Date (YYYY-MM-DD):"));
        formPanel.add(visitDateField);
        formPanel.add(new JLabel("Diagnosis:"));
        formPanel.add(new JScrollPane(diagnosisArea));
        formPanel.add(new JLabel("Prescription:"));
        formPanel.add(new JScrollPane(prescriptionArea));
        formPanel.add(new JLabel("--- Generate Bill ---"));
        formPanel.add(new JLabel());
        formPanel.add(new JLabel("Bill Amount:"));
        formPanel.add(billAmountField);
        formPanel.add(new JLabel("Bill Details (e.g., Consultation):"));
        formPanel.add(billDetailsField);

        JButton submitButton = new JButton("Submit Record and Bill");
        submitButton.addActionListener(e -> {
            String selectedPatient = (String) patientComboBox.getSelectedItem();
            int patientId = Integer.parseInt(selectedPatient.split(":")[0]);

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);

                // Add medical record
                String medicalSql = "INSERT INTO medicals (patient_id, doctor_id, visit_date, diagnosis, prescription) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement medicalPs = conn.prepareStatement(medicalSql);
                medicalPs.setInt(1, patientId);
                medicalPs.setInt(2, doctorId);
                medicalPs.setDate(3, java.sql.Date.valueOf(visitDateField.getText()));
                medicalPs.setString(4, diagnosisArea.getText());
                medicalPs.setString(5, prescriptionArea.getText());
                medicalPs.executeUpdate();

                // Add bill
                String billSql = "INSERT INTO bills (patient_id, amount, bill_date, details) VALUES (?, ?, ?, ?)";
                PreparedStatement billPs = conn.prepareStatement(billSql);
                billPs.setInt(1, patientId);
                billPs.setDouble(2, Double.parseDouble(billAmountField.getText()));
                billPs.setDate(3, java.sql.Date.valueOf(visitDateField.getText())); // Using visit date as bill date
                billPs.setString(4, billDetailsField.getText());
                billPs.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this, "Medical record and bill added successfully.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to add record/bill. Check inputs.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(submitButton, BorderLayout.SOUTH);

        return panel;
    }
}