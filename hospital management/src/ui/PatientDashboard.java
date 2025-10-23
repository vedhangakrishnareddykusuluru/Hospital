package ui;

import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PatientDashboard extends JFrame {
    private String patientUsername;
    private int patientId;

    public PatientDashboard(String username) {
        this.patientUsername = username;
        this.patientId = getPatientIdFromUsername(username);

        setTitle("Patient Dashboard - Welcome " + username);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("My Appointments", createViewPanel("appointments"));
        tabbedPane.add("My Medical Records", createViewPanel("medicals"));
        tabbedPane.add("My Bills", createViewPanel("bills"));
        tabbedPane.add("Book Appointment", createBookingPanel());

        add(tabbedPane);
    }

    private int getPatientIdFromUsername(String username) {
        String sql = "SELECT patient_id FROM patients WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("patient_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private JPanel createViewPanel(String viewType) {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "";
            switch (viewType) {
                case "appointments":
                    sql = "SELECT a.appointment_date, d.name as doctor_name, d.specialization, a.status FROM appointments a " +
                            "JOIN doctors d ON a.doctor_id = d.doctor_id WHERE a.patient_id = ?";
                    break;
                case "medicals":
                    sql = "SELECT m.visit_date, d.name as doctor_name, m.diagnosis, m.prescription FROM medicals m " +
                            "JOIN doctors d ON m.doctor_id = d.doctor_id WHERE m.patient_id = ?";
                    break;
                case "bills":
                    sql = "SELECT bill_date, details, amount, is_paid FROM bills WHERE patient_id = ?";
                    break;
            }

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();

            // Columns
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                model.addColumn(metaData.getColumnLabel(i));
            }

            // Rows
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                model.addRow(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return panel;
    }

    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JComboBox<String> doctorComboBox = new JComboBox<>();
        JTextField dateField = new JTextField(); // Format: YYYY-MM-DD

        // Populate doctors
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT doctor_id, name, specialization FROM doctors")) {
            while (rs.next()) {
                doctorComboBox.addItem(rs.getInt("doctor_id") + ": " + rs.getString("name") + " (" + rs.getString("specialization") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        panel.add(new JLabel("Select Doctor:"));
        panel.add(doctorComboBox);
        panel.add(new JLabel("Enter Date (YYYY-MM-DD):"));
        panel.add(dateField);

        JButton bookButton = new JButton("Book Appointment");
        panel.add(new JLabel()); // Placeholder
        panel.add(bookButton);

        bookButton.addActionListener(e -> {
            String selectedDoctor = (String) doctorComboBox.getSelectedItem();
            int doctorId = Integer.parseInt(selectedDoctor.split(":")[0]);
            String date = dateField.getText();

            String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_date) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, patientId);
                ps.setInt(2, doctorId);
                ps.setDate(3, java.sql.Date.valueOf(date));
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Appointment booked successfully!");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to book appointment.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }
}