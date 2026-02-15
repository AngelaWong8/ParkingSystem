package ui;

import facade.ParkingSystemFacade;
import model.*;
import dao.*;
import database.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.*;
import java.util.List;

public class AdminPanel extends JPanel {

    private ParkingSystemFacade facade;

    // Stats Labels
    private JLabel totalSpotsLabel;
    private JLabel occupiedLabel;
    private JLabel availableLabel;
    private JLabel occupancyRateLabel;
    private JLabel todayRevenueLabel;
    private JLabel todayFinesLabel;

    // Vehicle & Fines Display
    private JTextArea currentVehiclesArea;
    private JTextArea unpaidFinesArea;

    // Fine Scheme Selection
    private JComboBox<String> fineSchemeCombo;
    private JButton applySchemeBtn;

    // Test Mode
    private JCheckBox testModeCheckBox;
    private JSpinner testHoursSpinner;

    public AdminPanel(ParkingSystemFacade facade) {
        this.facade = facade;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initUI();
        facade.addRefreshListener(this::refreshData);
        refreshData();
    }

    private void initUI() {
        // Top: Statistics Panel
        JPanel statsPanel = createStatsPanel();
        add(statsPanel, BorderLayout.NORTH);

        // Center: Vehicles and Fines Panel
        JPanel centerPanel = createVehicleAndFinesPanel();
        add(centerPanel, BorderLayout.CENTER);

        // Bottom: Fine Scheme, Test Mode, and Refresh
        JPanel bottomPanel = new JPanel(new GridLayout(3, 1));
        bottomPanel.add(createFineSchemePanel());
        bottomPanel.add(createTestModePanel());
        bottomPanel.add(createButtonPanel());

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 15, 15));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "System Statistics",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14)
        ));

        panel.add(new JLabel("Total Spots:", SwingConstants.RIGHT));
        totalSpotsLabel = new JLabel("0", SwingConstants.LEFT);
        totalSpotsLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        panel.add(totalSpotsLabel);

        panel.add(new JLabel("Occupied:", SwingConstants.RIGHT));
        occupiedLabel = new JLabel("0", SwingConstants.LEFT);
        occupiedLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        occupiedLabel.setForeground(Color.RED);
        panel.add(occupiedLabel);

        panel.add(new JLabel("Available:", SwingConstants.RIGHT));
        availableLabel = new JLabel("0", SwingConstants.LEFT);
        availableLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        availableLabel.setForeground(Color.GREEN.darker());
        panel.add(availableLabel);

        panel.add(new JLabel("Occupancy Rate:", SwingConstants.RIGHT));
        occupancyRateLabel = new JLabel("0%", SwingConstants.LEFT);
        occupancyRateLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        panel.add(occupancyRateLabel);

        panel.add(new JLabel("Today's Revenue:", SwingConstants.RIGHT));
        todayRevenueLabel = new JLabel("RM 0.00", SwingConstants.LEFT);
        todayRevenueLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        todayRevenueLabel.setForeground(new Color(0, 150, 0));
        panel.add(todayRevenueLabel);

        panel.add(new JLabel("Today's Fines:", SwingConstants.RIGHT));
        todayFinesLabel = new JLabel("RM 0.00", SwingConstants.LEFT);
        todayFinesLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        todayFinesLabel.setForeground(Color.RED.darker());
        panel.add(todayFinesLabel);

        return panel;
    }

    private JPanel createVehicleAndFinesPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));

        // Current Vehicles Panel
        JPanel vehiclesPanel = new JPanel(new BorderLayout());
        vehiclesPanel.setBorder(BorderFactory.createTitledBorder("Vehicles Currently Parked"));
        currentVehiclesArea = new JTextArea(10, 30);
        currentVehiclesArea.setEditable(false);
        currentVehiclesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        vehiclesPanel.add(new JScrollPane(currentVehiclesArea), BorderLayout.CENTER);

        // Unpaid Fines Panel
        JPanel finesPanel = new JPanel(new BorderLayout());
        finesPanel.setBorder(BorderFactory.createTitledBorder("Unpaid Fines"));
        unpaidFinesArea = new JTextArea(10, 30);
        unpaidFinesArea.setEditable(false);
        unpaidFinesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        unpaidFinesArea.setForeground(Color.RED);
        finesPanel.add(new JScrollPane(unpaidFinesArea), BorderLayout.CENTER);

        panel.add(vehiclesPanel);
        panel.add(finesPanel);
        return panel;
    }

    private JPanel createFineSchemePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Fine Scheme Selection"));

        panel.add(new JLabel("Select Fine Scheme:"));
        fineSchemeCombo = new JComboBox<>(new String[]{
                "Fixed (RM 50)",
                "Progressive",
                "Hourly (RM 20/hr)"
        });
        applySchemeBtn = new JButton("Apply Scheme");

        panel.add(fineSchemeCombo);
        panel.add(applySchemeBtn);

        applySchemeBtn.addActionListener(e -> {
            String selected = (String) fineSchemeCombo.getSelectedItem();

            // Save to database
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)")) {
                pstmt.setString(1, "fine_scheme");
                pstmt.setString(2, selected);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this,
                        "Fine scheme set to: " + selected + "\nWill apply to future entries only",
                        "Scheme Updated",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error saving scheme: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createTestModePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Test Mode (For Demo Only)"));

        testModeCheckBox = new JCheckBox("Enable Test Mode");
        testHoursSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 72, 1));
        testHoursSpinner.setEnabled(false);

        testModeCheckBox.addActionListener(e -> {
            testHoursSpinner.setEnabled(testModeCheckBox.isSelected());
            // Save to system properties
            System.setProperty("test.mode", String.valueOf(testModeCheckBox.isSelected()));
            System.setProperty("test.hours", testHoursSpinner.getValue().toString());
        });

        testHoursSpinner.addChangeListener(e -> {
            System.setProperty("test.hours", testHoursSpinner.getValue().toString());
        });

        panel.add(testModeCheckBox);
        panel.add(new JLabel("Simulate Hours:"));
        panel.add(testHoursSpinner);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshBtn = new JButton("Refresh Data");
        refreshBtn.addActionListener(e -> refreshData());
        panel.add(refreshBtn);
        return panel;
    }

    private void refreshData() {
        try {
            // Update statistics
            int total = facade.getTotalSpots();
            int occupied = facade.getCurrentOccupancy();
            int available = total - occupied;
            double rate = facade.getOccupancyRate();
            double revenue = facade.getTotalRevenueToday();
            double fines = facade.getTotalFinesToday();

            totalSpotsLabel.setText(String.valueOf(total));
            occupiedLabel.setText(String.valueOf(occupied));
            availableLabel.setText(String.valueOf(available));
            occupancyRateLabel.setText(String.format("%.1f%%", rate));
            todayRevenueLabel.setText(String.format("RM %.2f", revenue));
            todayFinesLabel.setText(String.format("RM %.2f", fines));

            // Update current vehicles list
            refreshCurrentVehicles();

            // Update unpaid fines
            refreshUnpaidFines();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error refreshing data: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshCurrentVehicles() {
        try {
            List<Ticket> tickets = new TicketDAO().getAllActiveTickets();
            StringBuilder sb = new StringBuilder();
            sb.append(" LICENSE PLATE | SPOT   | ENTRY TIME\n");
            sb.append("----------------------------------------\n");

            for (Ticket t : tickets) {
                String time = t.getEntryTime().format(
                        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                sb.append(String.format(" %-14s | %-6s | %s\n",
                        t.getLicensePlate(), t.getSpotId(), time));
            }

            if (tickets.isEmpty()) {
                sb.append("\n No vehicles currently parked.");
            }

            currentVehiclesArea.setText(sb.toString());

        } catch (SQLException e) {
            currentVehiclesArea.setText("Error loading vehicles: " + e.getMessage());
        }
    }

    private void refreshUnpaidFines() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(" LICENSE PLATE | FINE AMOUNT\n");
            sb.append("--------------------------------\n");

            String sql = "SELECT license_plate, SUM(fine_amount) as total FROM fines WHERE is_paid = 0 GROUP BY license_plate";
            try (Connection conn = DatabaseConnection.connect();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                double totalOutstanding = 0;
                while (rs.next()) {
                    double amount = rs.getDouble("total");
                    totalOutstanding += amount;
                    sb.append(String.format(" %-14s | RM %.2f\n",
                            rs.getString("license_plate"), amount));
                }

                sb.append("\n");
                sb.append(String.format(" TOTAL OUTSTANDING: RM %.2f", totalOutstanding));

                if (totalOutstanding == 0) {
                    sb.append("\n No unpaid fines.");
                }
            }

            unpaidFinesArea.setText(sb.toString());

        } catch (Exception e) {
            unpaidFinesArea.setText("Error loading fines: " + e.getMessage());
        }
    }
}
