package ui;

import facade.ParkingSystemFacade;
import model.*;
import dao.*;
import database.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminPanel extends JPanel {

    private final ParkingSystemFacade facade;
    private static final Logger LOGGER = Logger.getLogger(AdminPanel.class.getName());

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
        finesPanel.setBorder(BorderFactory.createTitledBorder("Unpaid Fines by Type"));
        unpaidFinesArea = new JTextArea(10, 30);
        unpaidFinesArea.setEditable(false);
        unpaidFinesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        finesPanel.add(new JScrollPane(unpaidFinesArea), BorderLayout.CENTER);

        panel.add(vehiclesPanel);
        panel.add(finesPanel);
        return panel;
    }

    private JPanel createFineSchemePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Fine Scheme Selection (For Overstay Fines Only)"));

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
            String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)";
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, "fine_scheme");
                pstmt.setString(2, selected);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this,
                        "Fine scheme set to: " + selected + "\nWill apply to future overstay fines only",
                        "Scheme Updated",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error saving fine scheme", ex);
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

        testHoursSpinner.addChangeListener(e ->
                System.setProperty("test.hours", testHoursSpinner.getValue().toString())
        );

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
            ReportDAO reportDAO = new ReportDAO();

            // Update statistics
            int total = facade.getTotalSpots();
            int occupied = facade.getCurrentOccupancy();
            int available = total - occupied;
            double rate = facade.getOccupancyRate();

            // Get fresh revenue and fines data
            double revenue = reportDAO.getTotalRevenueToday();
            double fines = reportDAO.getTotalFinesToday();

            // Debug output
            debugPaymentsToday();

            totalSpotsLabel.setText(String.valueOf(total));
            occupiedLabel.setText(String.valueOf(occupied));
            availableLabel.setText(String.valueOf(available));
            occupancyRateLabel.setText(String.format("%.1f%%", rate));
            todayRevenueLabel.setText(String.format("RM %.2f", revenue));
            todayFinesLabel.setText(String.format("RM %.2f", fines));

            // Update current vehicles list
            refreshCurrentVehicles();

            // Update unpaid fines with categories
            refreshUnpaidFines();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error refreshing admin data", e);
            JOptionPane.showMessageDialog(this,
                    "Error refreshing data: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void debugPaymentsToday() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String sql = "SELECT payment_method, amount_paid FROM payments WHERE date(payment_time) = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, today);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n=== DEBUG: Payments today ===");
                boolean hasPayments = false;
                while (rs.next()) {
                    hasPayments = true;
                    System.out.println("  " + rs.getString("payment_method") +
                            ": RM " + rs.getDouble("amount_paid"));
                }
                if (!hasPayments) {
                    System.out.println("  No payments today");
                }
                System.out.println("===========================\n");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error debugging payments", e);
        }
    }

    private void refreshCurrentVehicles() {
        String sql = "SELECT * FROM tickets";
        StringBuilder sb = new StringBuilder();
        sb.append(" LICENSE PLATE | SPOT   | ENTRY TIME\n");
        sb.append("----------------------------------------\n");

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasVehicles = false;
            while (rs.next()) {
                hasVehicles = true;
                String plate = rs.getString("license_plate");
                String spotId = rs.getString("spot_id");
                String entryTime = rs.getString("entry_time");

                // Format time to show only time part
                if (entryTime != null && entryTime.length() > 11) {
                    entryTime = entryTime.substring(11, 19);
                }

                sb.append(String.format(" %-14s | %-6s | %s\n", plate, spotId, entryTime));
            }

            if (!hasVehicles) {
                sb.append("\n No vehicles currently parked.");
            }

            currentVehiclesArea.setText(sb.toString());

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error refreshing vehicles", e);
            currentVehiclesArea.setText("Error loading vehicles: " + e.getMessage());
        }
    }

    private void refreshUnpaidFines() {
        StringBuilder sb = new StringBuilder();

        String sql = "SELECT license_plate, fine_type, fine_amount, paid_amount, " +
                "(fine_amount - paid_amount) as outstanding, overstay_minutes, calculation_method " +
                "FROM fines WHERE is_paid = 0 " +
                "ORDER BY license_plate, created_date";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            double totalOverstay = 0;
            double totalReservation = 0;
            double totalUnpaid = 0;
            double totalOutstanding = 0;

            sb.append(" LICENSE PLATE | FINE TYPE          | TOTAL  | PAID  | OUTSTANDING | DETAILS\n");
            sb.append("--------------------------------------------------------------------------------\n");

            boolean hasFines = false;
            while (rs.next()) {
                hasFines = true;
                String plate = rs.getString("license_plate");
                String fineType = rs.getString("fine_type");
                double fineAmount = rs.getDouble("fine_amount");
                double paidAmount = rs.getDouble("paid_amount");
                double outstanding = rs.getDouble("outstanding");
                long overstayMins = rs.getLong("overstay_minutes");
                String method = rs.getString("calculation_method");

                totalOutstanding += outstanding;

                // Add to category totals
                switch (fineType) {
                    case "OVERSTAY":
                        totalOverstay += outstanding;
                        break;
                    case "RESERVATION_MISUSE":
                        totalReservation += outstanding;
                        break;
                    case "UNPAID_FEE":
                        totalUnpaid += outstanding;
                        break;
                    default:
                        break;
                }

                // Format fine type for display
                String displayType;
                String details;
                switch (fineType) {
                    case "OVERSTAY":
                        displayType = "Overstay";
                        details = overstayMins + " mins, " + (method != null ? method : "Fixed");
                        break;
                    case "RESERVATION_MISUSE":
                        displayType = "Reservation Misuse";
                        details = "RM50 fine";
                        break;
                    case "UNPAID_FEE":
                        displayType = "Unpaid Balance";
                        details = "Previous parking fee";
                        break;
                    default:
                        displayType = fineType;
                        details = "";
                }

                sb.append(String.format(" %-14s | %-18s | RM %-6.2f| RM %-5.2f| RM %-9.2f| %s\n",
                        plate, displayType, fineAmount, paidAmount, outstanding, details));
            }

            if (!hasFines) {
                sb.append("\n No unpaid fines found.\n\n");
            }

            sb.append("\n");
            sb.append("-".repeat(80)).append("\n");
            sb.append("SUMMARY BY FINE TYPE:\n");
            sb.append(String.format("  Overstay Fines:          RM %.2f\n", totalOverstay));
            sb.append(String.format("  Reservation Misuse Fines: RM %.2f\n", totalReservation));
            sb.append(String.format("  Unpaid Balance Fines:    RM %.2f\n", totalUnpaid));
            sb.append("-".repeat(80)).append("\n");
            sb.append(String.format("  TOTAL OUTSTANDING:       RM %.2f\n", totalOutstanding));

            unpaidFinesArea.setText(sb.toString());

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error refreshing unpaid fines", e);
            unpaidFinesArea.setText("Error loading fines: " + e.getMessage());
        }
    }
}
