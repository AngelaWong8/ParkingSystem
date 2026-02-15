package ui;

import facade.ParkingSystemFacade;
import model.*;
import dao.*;
import database.DatabaseConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExitPanel extends JPanel {
    private ParkingSystemFacade facade;
    private JTextField plateField = new JTextField(15);
    private JTextArea receiptArea = new JTextArea(20, 40);
    private JComboBox<String> paymentMethodCombo;
    private JTextField amountPaidField = new JTextField(10);
    private FineStrategy fineStrategy = new FixedFineStrategy(); // Default
    private static final Logger LOGGER = Logger.getLogger(ExitPanel.class.getName());

    // Test mode
    private boolean testMode = false;
    private int testHours = 25;

    public ExitPanel(ParkingSystemFacade facade) {
        this.facade = facade;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initUI();

        // Register for refresh
        facade.addRefreshListener(this::clearScreen);
    }

    private void loadFineScheme() {
        String sql = "SELECT value FROM settings WHERE key = 'fine_scheme'";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String scheme = rs.getString("value");
                LOGGER.info("Loading fine scheme: " + scheme);

                if (scheme.contains("Fixed")) {
                    fineStrategy = new FixedFineStrategy();
                    LOGGER.info("Using Fixed Fine Strategy");
                } else if (scheme.contains("Progressive")) {
                    fineStrategy = new ProgressiveFineStrategy();
                    LOGGER.info("Using Progressive Fine Strategy");
                } else if (scheme.contains("Hourly")) {
                    fineStrategy = new HourlyFineStrategy();
                    LOGGER.info("Using Hourly Fine Strategy");
                }
            } else {
                LOGGER.info("No fine scheme found in database, using default Fixed");
                fineStrategy = new FixedFineStrategy();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error loading fine scheme, using default Fixed", e);
            fineStrategy = new FixedFineStrategy();
        }
    }

    private void initUI() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // License plate
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("License Plate:"), gbc);
        gbc.gridx = 1;
        plateField.setToolTipText("Enter license plate (case insensitive)");
        inputPanel.add(plateField, gbc);

        // Payment method
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Payment Method:"), gbc);
        gbc.gridx = 1;
        paymentMethodCombo = new JComboBox<>(new String[]{"Cash", "Card"});
        inputPanel.add(paymentMethodCombo, gbc);

        // Amount paid (for partial payments)
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Amount Paid:"), gbc);
        gbc.gridx = 1;
        amountPaidField.setText("0.00");
        inputPanel.add(amountPaidField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton checkBtn = new JButton("Calculate Fees");
        JButton payBtn = new JButton("Process Payment");
        payBtn.setBackground(new Color(34, 139, 34));
        payBtn.setForeground(Color.WHITE);

        buttonPanel.add(checkBtn);
        buttonPanel.add(payBtn);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        inputPanel.add(buttonPanel, gbc);

        // Receipt area
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        receiptArea.setEditable(false);
        receiptArea.setBackground(new Color(245, 245, 245));
        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.setPreferredSize(new Dimension(600, 300));

        // Test mode panel (for demo only)
        JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        testPanel.setBorder(BorderFactory.createTitledBorder("Test Mode (For Demo Only)"));
        JCheckBox testModeCheck = new JCheckBox("Enable Test Mode");
        JSpinner testHoursSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 72, 1));
        testHoursSpinner.setEnabled(false);

        testModeCheck.addActionListener(e -> {
            testMode = testModeCheck.isSelected();
            testHoursSpinner.setEnabled(testMode);
        });
        testHoursSpinner.addChangeListener(e -> testHours = (int) testHoursSpinner.getValue());

        testPanel.add(testModeCheck);
        testPanel.add(new JLabel("Simulate Hours:"));
        testPanel.add(testHoursSpinner);

        // Button actions
        checkBtn.addActionListener(e -> calculateExit());
        payBtn.addActionListener(e -> processPayment());

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.NORTH);
        topPanel.add(testPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void calculateExit() {
        // Load the current fine scheme from database before calculating
        loadFineScheme();

        try {
            String plate = plateField.getText().trim().toUpperCase();
            if (plate.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter license plate");
                return;
            }

            Ticket ticket = facade.findActiveTicket(plate);

            // Get unpaid fines from previous visits
            double unpaidFines = getUnpaidFines(plate);

            if (ticket == null) {
                // No active ticket, but might have unpaid fines
                if (unpaidFines > 0) {
                    StringBuilder receipt = new StringBuilder();
                    receipt.append("=".repeat(60)).append("\n");
                    receipt.append("              OUTSTANDING FINES NOTICE\n");
                    receipt.append("=".repeat(60)).append("\n\n");
                    receipt.append("License Plate: ").append(plate).append("\n");
                    receipt.append("No active parking ticket found.\n\n");
                    receipt.append(String.format("Outstanding fines from previous visits: RM %.2f\n", unpaidFines));
                    receipt.append("\nPlease pay these fines before parking again.\n");
                    receipt.append("=".repeat(60));

                    receiptArea.setText(receipt.toString());

                    // Store for payment processing
                    putClientProperty("totalDue", unpaidFines);
                    putClientProperty("unpaidFines", unpaidFines);
                    putClientProperty("ticket", null);
                    putClientProperty("spot", null);
                } else {
                    receiptArea.setText("NO ACTIVE TICKET OR FINES FOUND FOR: " + plate);
                }
                return;
            }

            // Time calculation for active ticket
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime entry = ticket.getEntryTime();

            if (testMode) {
                entry = now.minusHours(testHours);
            }

            long mins = Duration.between(entry, now).toMinutes();
            long hours = (long) Math.ceil(mins / 60.0);
            if (hours == 0) hours = 1;

            // Get spot and vehicle
            ParkingSpot spot = new ParkingSpotDAO().getSpotBySpotId(ticket.getSpotId());
            Vehicle vehicle = facade.findVehicle(plate);

            // Calculate base fee with handicapped override
            double rate = (spot != null) ? spot.getHourlyRate() : 5.0;
            String rateDescription = String.format("RM %.2f/hr", rate);

            // Apply handicapped discounts - OVERRIDE for handicapped vehicles
            if (vehicle instanceof HandicappedVehicle) {
                HandicappedVehicle hv = (HandicappedVehicle) vehicle;

                if (hv.hasHandicappedCard()) {
                    // Card holder gets special rates
                    if (spot != null && spot.getType().equalsIgnoreCase("Handicapped")) {
                        rate = 0.0; // Card + Handicapped Spot = FREE
                        rateDescription = "FREE (Handicapped Card + Handicapped Spot)";
                    } else {
                        rate = 2.0; // Card in any other spot = RM 2.00 (override)
                        rateDescription = "RM 2.00/hr (Handicapped Card - Special Rate)";
                    }
                } else {
                    // Handicapped vehicle without card - still gets RM2/hr in any spot
                    rate = 2.0; // All handicapped vehicles get RM2/hr regardless of spot
                    rateDescription = "RM 2.00/hr (Handicapped Vehicle - Special Rate)";
                }
            }

            double baseFee = hours * rate;

            // Calculate fines - only apply to time beyond 24 hours (1440 minutes)
            double currentFine = 0;
            String fineDescription = "";
            long totalMins = mins;
            long overstayMins = Math.max(0, totalMins - 1440); // Minutes beyond 24 hours
            String fineType = "";

            // Pass ONLY the overstay minutes to the fine strategy
            currentFine = fineStrategy.calculateFine(overstayMins);

            if (currentFine > 0) {
                long totalHours = (long) Math.ceil(totalMins / 60.0);
                long overstayHours = (long) Math.ceil(overstayMins / 60.0);

                if (fineStrategy instanceof HourlyFineStrategy) {
                    fineDescription = String.format("Overstay fine (%d hrs over 24hr limit × RM20/hr) - %s",
                            overstayHours, fineStrategy.getSchemeName());
                } else if (fineStrategy instanceof ProgressiveFineStrategy) {
                    fineDescription = "Overstay fine (progressive rate) - " + fineStrategy.getSchemeName();
                } else {
                    fineDescription = String.format("Overstay fine (%d hrs over 24hr limit) - %s",
                            overstayHours, fineStrategy.getSchemeName());
                }
                fineType = "OVERSTAY";
                System.out.println("Total minutes: " + totalMins + ", Overstay minutes: " + overstayMins +
                        ", Fine: " + currentFine + " using " + fineStrategy.getSchemeName());
            }

            // Reserved spot misuse check using ReservationDAO
            if (spot != null && spot.getType().equalsIgnoreCase("Reserved")) {
                ReservationDAO reservationDAO = new ReservationDAO();
                boolean hasReservation = reservationDAO.hasActiveReservation(plate, spot.getSpotId());

                if (!hasReservation) {
                    currentFine += 50.0;
                    if (fineDescription.isEmpty()) {
                        fineDescription = "Reserved spot without reservation (RM 50 fine)";
                        fineType = "RESERVATION_MISUSE";
                    } else {
                        fineDescription += " + Reserved spot fine (RM 50)";
                        fineType = "MULTIPLE";
                    }
                }
            }

            double total = baseFee + currentFine + unpaidFines;

            // Format receipt
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            StringBuilder receipt = new StringBuilder();
            receipt.append("=".repeat(60)).append("\n");
            receipt.append("                    EXIT RECEIPT\n");
            receipt.append("=".repeat(60)).append("\n\n");

            receipt.append("License Plate: ").append(plate).append("\n");
            receipt.append("Spot: ").append(ticket.getSpotId()).append(" (").append(spot != null ? spot.getType() : "Unknown").append(")\n\n");

            receipt.append("Entry Time: ").append(entry.format(dtf)).append("\n");
            receipt.append("Exit Time:  ").append(now.format(dtf)).append("\n");
            receipt.append("Duration:   ").append(mins).append(" minutes (").append(hours).append(" hours)\n\n");

            receipt.append("-".repeat(60)).append("\n");
            receipt.append("PARKING FEE BREAKDOWN:\n");
            receipt.append(String.format("  %d hours × %s = RM %.2f\n", hours, rateDescription, baseFee));

            if (currentFine > 0) {
                receipt.append("\nCURRENT VISIT FINES:\n");
                receipt.append(String.format("  %s: RM %.2f\n", fineDescription, currentFine));

                // Add detailed breakdown for hourly fine
                if (fineStrategy instanceof HourlyFineStrategy && overstayMins > 0) {
                    long overstayHours = (long) Math.ceil(overstayMins / 60.0);
                    receipt.append(String.format("  (First 24 hours: no fine, Next %d hours: %d × RM20 = RM%.2f)\n",
                            overstayHours, overstayHours, overstayHours * 20.0));
                }
                // Add breakdown for progressive fine
                else if (fineStrategy instanceof ProgressiveFineStrategy && overstayMins > 0) {
                    receipt.append("  (First 24h: RM50, 24-48h: +RM100, 48-72h: +RM150, >72h: +RM200)\n");
                }
            }

            if (unpaidFines > 0) {
                receipt.append("\nPREVIOUS UNPAID FINES:\n");
                receipt.append(String.format("  Total outstanding: RM %.2f\n", unpaidFines));
            }

            receipt.append("-".repeat(60)).append("\n");
            receipt.append(String.format("TOTAL AMOUNT DUE: RM %.2f\n\n", total));

            if (testMode) {
                receipt.append("*** TEST MODE: Simulated " + testHours + " hour stay ***\n");
            }

            if (total == 0) {
                receipt.append("✅ NO PAYMENT REQUIRED - Gate will open automatically\n");
            }

            receipt.append("=".repeat(60));

            receiptArea.setText(receipt.toString());

            // Store all data for payment processing
            putClientProperty("totalDue", total);
            putClientProperty("baseFee", baseFee);
            putClientProperty("currentFine", currentFine);
            putClientProperty("unpaidFines", unpaidFines);
            putClientProperty("ticket", ticket);
            putClientProperty("spot", spot);
            putClientProperty("overstayMins", overstayMins);
            putClientProperty("fineDescription", fineDescription);
            putClientProperty("fineType", fineType);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void processPayment() {
        if (receiptArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please calculate fees first!");
            return;
        }

        try {
            String plate = plateField.getText().trim().toUpperCase();
            String method = (String) paymentMethodCombo.getSelectedItem();

            double totalDue = (double) getClientProperty("totalDue");

            // Handle FREE parking (totalDue = 0)
            if (totalDue == 0) {
                processFreeExit(plate, method);
                return;
            }

            double amountPaid;

            try {
                amountPaid = Double.parseDouble(amountPaidField.getText().trim());
                if (amountPaid <= 0) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter a valid amount. Total due: RM " + String.format("%.2f", totalDue),
                            "Payment Required",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid amount. Total due: RM " + String.format("%.2f", totalDue),
                        "Invalid Amount",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Ticket ticket = (Ticket) getClientProperty("ticket");
            ParkingSpot spot = (ParkingSpot) getClientProperty("spot");
            double currentFine = (double) getClientProperty("currentFine");
            double unpaidFines = (double) getClientProperty("unpaidFines");
            double baseFee = (double) getClientProperty("baseFee");
            long overstayMins = (long) getClientProperty("overstayMins");
            String fineType = (String) getClientProperty("fineType");

            PaymentDAO paymentDAO = new PaymentDAO();
            double originalAmountPaid = amountPaid;
            double remainingToAllocate = amountPaid;

            // Track payments
            double paidTowardBaseFee = 0;
            double paidTowardCurrentFine = 0;
            double paidTowardOldFines = 0;

            // PRIORITY 1: Pay current parking fee first
            if (remainingToAllocate > 0 && baseFee > 0) {
                paidTowardBaseFee = Math.min(remainingToAllocate, baseFee);
                if (paidTowardBaseFee > 0) {
                    paymentDAO.savePayment(ticket.getTicketId(), plate, paidTowardBaseFee, method);
                    System.out.println("Paid RM " + paidTowardBaseFee + " toward parking fee");
                }
                remainingToAllocate -= paidTowardBaseFee;
            }

            // PRIORITY 2: Pay current fines next
            if (remainingToAllocate > 0 && currentFine > 0) {
                paidTowardCurrentFine = Math.min(remainingToAllocate, currentFine);
                if (paidTowardCurrentFine > 0) {
                    paymentDAO.savePayment(ticket.getTicketId(), plate, paidTowardCurrentFine,
                            method + " - FINE (" + fineType + ")");
                    System.out.println("Paid RM " + paidTowardCurrentFine + " toward current fine");
                }
                remainingToAllocate -= paidTowardCurrentFine;
            }

            // PRIORITY 3: Pay old fines last
            if (remainingToAllocate > 0 && unpaidFines > 0) {
                paidTowardOldFines = Math.min(remainingToAllocate, unpaidFines);
                if (paidTowardOldFines > 0) {
                    paymentDAO.savePayment("OLD-FINES-" + System.currentTimeMillis(), plate,
                            paidTowardOldFines, method + " - FINE (PREVIOUS)");
                    paymentDAO.updateFinePayment(plate, paidTowardOldFines);
                    System.out.println("Paid RM " + paidTowardOldFines + " toward old fines");
                }
                remainingToAllocate -= paidTowardOldFines;
            }

            // Calculate what's still owed
            double unpaidBaseFee = baseFee - paidTowardBaseFee;
            double unpaidCurrentFine = currentFine - paidTowardCurrentFine;
            double unpaidOldFines = unpaidFines - paidTowardOldFines;

            double remainingBalance = unpaidBaseFee + unpaidCurrentFine + unpaidOldFines;

            // Create fine records for any unpaid amounts from this visit
            if (unpaidBaseFee > 0) {
                paymentDAO.createFine(ticket.getTicketId(), plate, unpaidBaseFee, 0, "UNPAID_FEE", "Parking Fee");
                System.out.println("Created UNPAID_FEE fine for RM " + unpaidBaseFee);
            }

            if (unpaidCurrentFine > 0) {
                // Check if a fine already exists for this ticket
                String checkSql = "SELECT fine_id, paid_amount FROM fines WHERE ticket_id = ? AND fine_type = ?";
                try (Connection conn = DatabaseConnection.connect();
                     PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

                    checkStmt.setString(1, ticket.getTicketId());
                    checkStmt.setString(2, fineType);
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next()) {
                        // Update existing fine
                        String fineId = rs.getString("fine_id");
                        double existingPaid = rs.getDouble("paid_amount");
                        String updateSql = "UPDATE fines SET paid_amount = ?, fine_amount = ? WHERE fine_id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setDouble(1, existingPaid + paidTowardCurrentFine);
                            updateStmt.setDouble(2, currentFine);
                            updateStmt.setString(3, fineId);
                            updateStmt.executeUpdate();
                            System.out.println("Updated existing fine, paid: " + (existingPaid + paidTowardCurrentFine) +
                                    ", total: " + currentFine + ", outstanding: " + unpaidCurrentFine);
                        }
                    } else {
                        // Create new fine
                        paymentDAO.createFine(ticket.getTicketId(), plate, unpaidCurrentFine,
                                overstayMins, fineType, fineStrategy.getSchemeName());
                        System.out.println("Created new fine for RM " + unpaidCurrentFine);
                    }
                }
            }

            // Create receipt
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            StringBuilder paymentReceipt = new StringBuilder();
            paymentReceipt.append("=".repeat(60)).append("\n");
            paymentReceipt.append("                 PAYMENT CONFIRMATION\n");
            paymentReceipt.append("=".repeat(60)).append("\n\n");

            paymentReceipt.append("License Plate: ").append(plate).append("\n");
            paymentReceipt.append("Payment Method: ").append(method).append("\n\n");

            paymentReceipt.append(String.format("Total Due:       RM %.2f\n", totalDue));
            paymentReceipt.append(String.format("Amount Paid:     RM %.2f\n", originalAmountPaid));
            paymentReceipt.append("\nPAYMENT BREAKDOWN:\n");
            if (paidTowardBaseFee > 0) {
                paymentReceipt.append(String.format("  - Parking Fee:    RM %.2f\n", paidTowardBaseFee));
            }
            if (paidTowardCurrentFine > 0) {
                paymentReceipt.append(String.format("  - Current Fine:   RM %.2f\n", paidTowardCurrentFine));
            }
            if (paidTowardOldFines > 0) {
                paymentReceipt.append(String.format("  - Previous Fines: RM %.2f\n", paidTowardOldFines));
            }

            paymentReceipt.append("\nREMAINING BALANCE BREAKDOWN:\n");
            if (unpaidBaseFee > 0) {
                paymentReceipt.append(String.format("  - Unpaid Parking Fee: RM %.2f\n", unpaidBaseFee));
            }
            if (unpaidCurrentFine > 0) {
                paymentReceipt.append(String.format("  - Unpaid Current Fine: RM %.2f\n", unpaidCurrentFine));
            }
            if (unpaidOldFines > 0) {
                paymentReceipt.append(String.format("  - Unpaid Previous Fines: RM %.2f\n", unpaidOldFines));
            }

            paymentReceipt.append(String.format("\nTOTAL REMAINING: RM %.2f\n\n", remainingBalance));

            if (remainingBalance <= 0.01) { // Fully paid
                paymentReceipt.append("✓ PAID IN FULL\n");

                // Free up the spot
                if (spot != null && ticket != null) {
                    new ParkingSpotDAO().updateSpotStatus(spot.getDbId(), false);
                }

                // Remove ticket if exists
                if (ticket != null) {
                    new TicketDAO().removeTicket(plate);
                }

                JOptionPane.showMessageDialog(this,
                        "Payment successful! Gate opening.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

            } else {
                paymentReceipt.append("⚠ PARTIAL PAYMENT - Balance added to fines\n");

                // Free up the spot (they leave)
                if (spot != null && ticket != null) {
                    new ParkingSpotDAO().updateSpotStatus(spot.getDbId(), false);
                }

                // Remove ticket
                if (ticket != null) {
                    new TicketDAO().removeTicket(plate);
                }

                JOptionPane.showMessageDialog(this,
                        "Partial payment recorded. Remaining balance: RM " + String.format("%.2f", remainingBalance) +
                                "\nThis amount will be added to your fines.",
                        "Partial Payment",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            paymentReceipt.append("\n").append("=".repeat(60));
            receiptArea.setText(paymentReceipt.toString());

            // Force a refresh of the admin panel
            facade.notifyRefresh();

            clearScreen();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Payment Failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void processFreeExit(String plate, String method) {
        try {
            Ticket ticket = (Ticket) getClientProperty("ticket");
            ParkingSpot spot = (ParkingSpot) getClientProperty("spot");

            // Create a receipt for free exit
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            StringBuilder freeReceipt = new StringBuilder();
            freeReceipt.append("=".repeat(60)).append("\n");
            freeReceipt.append("              FREE EXIT - NO PAYMENT REQUIRED\n");
            freeReceipt.append("=".repeat(60)).append("\n\n");

            freeReceipt.append("License Plate: ").append(plate).append("\n");
            if (ticket != null) {
                freeReceipt.append("Spot: ").append(ticket.getSpotId()).append("\n");
                freeReceipt.append("Entry Time: ").append(ticket.getEntryTime().format(dtf)).append("\n");
                freeReceipt.append("Exit Time:  ").append(LocalDateTime.now().format(dtf)).append("\n");
            }

            freeReceipt.append("\n✅ NO PAYMENT REQUIRED\n");
            if (spot != null && spot.getType().equalsIgnoreCase("Handicapped")) {
                freeReceipt.append("(Handicapped card holder in Handicapped spot)\n");
            }

            freeReceipt.append("\nGate opening...\n");
            freeReceipt.append("=".repeat(60));

            receiptArea.setText(freeReceipt.toString());

            // Save a zero payment record for auditing
            if (ticket != null) {
                PaymentDAO paymentDAO = new PaymentDAO();
                paymentDAO.savePayment(ticket.getTicketId(), plate, 0.0, method + " (FREE)");
            }

            // Free up the spot
            if (spot != null && ticket != null) {
                new ParkingSpotDAO().updateSpotStatus(spot.getDbId(), false);
            }

            // Remove ticket
            if (ticket != null) {
                new TicketDAO().removeTicket(plate);
            }

            JOptionPane.showMessageDialog(this,
                    "Free exit processed. Gate opening.",
                    "Exit Successful",
                    JOptionPane.INFORMATION_MESSAGE);

            clearScreen();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error processing free exit: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private double getUnpaidFines(String plate) {
        try {
            return new PaymentDAO().getUnpaidFines(plate);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error getting unpaid fines for " + plate, e);
            return 0.0;
        }
    }

    private void clearScreen() {
        plateField.setText("");
        amountPaidField.setText("0.00");
        receiptArea.setText("");
    }
}
