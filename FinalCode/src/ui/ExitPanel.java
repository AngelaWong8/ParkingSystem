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

public class ExitPanel extends JPanel {
    private ParkingSystemFacade facade;
    private JTextField plateField = new JTextField(15);
    private JTextArea receiptArea = new JTextArea(20, 40);
    private JComboBox<String> paymentMethodCombo;
    private JTextField amountPaidField = new JTextField(10);
    private FineStrategy fineStrategy = new FixedFineStrategy(); // Default

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

        // Load saved fine scheme
        loadFineScheme();
    }

    private void loadFineScheme() {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement("SELECT value FROM settings WHERE key = 'fine_scheme'")) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String scheme = rs.getString("value");
                switch (scheme) {
                    case "Fixed (RM 50)":
                        fineStrategy = new FixedFineStrategy();
                        break;
                    case "Progressive":
                        fineStrategy = new ProgressiveFineStrategy();
                        break;
                    case "Hourly (RM 20/hr)":
                        fineStrategy = new HourlyFineStrategy();
                        break;
                }
            }
        } catch (SQLException e) {
            // Use default
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

            // Calculate base fee
            double rate = (spot != null) ? spot.getHourlyRate() : 5.0;
            String rateDescription = String.format("RM %.2f/hr", rate);

            // Apply handicapped discounts
            if (vehicle instanceof HandicappedVehicle) {
                HandicappedVehicle hv = (HandicappedVehicle) vehicle;
                String actualSpotType = (spot != null) ? spot.getType() : "Regular";

                if (hv.hasHandicappedCard()) {
                    if (actualSpotType.equalsIgnoreCase("Handicapped")) {
                        rate = 0.0; // Card + Handicapped Spot = FREE
                        rateDescription = "FREE (Handicapped Card + Handicapped Spot)";
                    } else {
                        rate = 2.0; // Card + Regular Spot = RM 2.00
                        rateDescription = "RM 2.00/hr (Handicapped Card)";
                    }
                }
            }

            double baseFee = hours * rate;

            // Calculate fines (after 24 hours = 1440 minutes)
            double currentFine = 0;
            String fineDescription = "";
            long overstayMins = 0;

            if (mins > 1440) {
                overstayMins = mins - 1440;
                currentFine = fineStrategy.calculateFine(overstayMins);
                fineDescription = "Overstay fine (" + (overstayMins/60) + " hrs over 24hr limit) - " + fineStrategy.getSchemeName();
            }

            // Reserved spot misuse check using ReservationDAO
            if (spot != null && spot.getType().equalsIgnoreCase("Reserved")) {
                ReservationDAO reservationDAO = new ReservationDAO();
                boolean hasReservation = reservationDAO.hasActiveReservation(plate, spot.getSpotId());

                if (!hasReservation) {
                    currentFine += 50.0;
                    if (fineDescription.isEmpty()) {
                        fineDescription = "Reserved spot without reservation (RM 50 fine)";
                    } else {
                        fineDescription += " + Reserved spot fine (RM 50)";
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

            PaymentDAO paymentDAO = new PaymentDAO();

            // First, apply payment to unpaid fines from previous visits (oldest first)
            if (unpaidFines > 0) {
                double paymentForOldFines = Math.min(amountPaid, unpaidFines);
                if (paymentForOldFines > 0) {
                    paymentDAO.updateFinePayment(plate, paymentForOldFines);
                }
                amountPaid -= paymentForOldFines;
            }

            // Then apply to current fines if any
            if (amountPaid > 0 && currentFine > 0) {
                double paymentForCurrentFine = Math.min(amountPaid, currentFine);
                // If they don't pay the full current fine, create a fine record
                if (paymentForCurrentFine < currentFine) {
                    double remainingFine = currentFine - paymentForCurrentFine;
                    // FIXED: Call createFine with 5 parameters to match your PaymentDAO
                    paymentDAO.createFine(ticket.getTicketId(), plate, remainingFine, overstayMins, fineStrategy.getSchemeName());
                }
                amountPaid -= paymentForCurrentFine;
            }

            // Finally apply to base parking fee
            if (amountPaid > 0 && baseFee > 0) {
                double paymentForBaseFee = Math.min(amountPaid, baseFee);
                double remainingBaseFee = baseFee - paymentForBaseFee;

                // Save the payment
                if (paymentForBaseFee > 0) {
                    paymentDAO.savePayment(ticket.getTicketId(), plate, paymentForBaseFee, method);
                }

                // If they didn't pay the full base fee, create a fine for the remainder
                if (remainingBaseFee > 0) {
                    // FIXED: Call createFine with 5 parameters
                    paymentDAO.createFine(ticket.getTicketId(), plate, remainingBaseFee, 0, "Unpaid Parking Fee");
                }
            }

            double amountEntered = Double.parseDouble(amountPaidField.getText().trim());
            double remainingBalance = totalDue - amountEntered;

            // Create receipt
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            StringBuilder paymentReceipt = new StringBuilder();
            paymentReceipt.append("=".repeat(60)).append("\n");
            paymentReceipt.append("                 PAYMENT CONFIRMATION\n");
            paymentReceipt.append("=".repeat(60)).append("\n\n");

            paymentReceipt.append("License Plate: ").append(plate).append("\n");
            paymentReceipt.append("Payment Method: ").append(method).append("\n\n");

            paymentReceipt.append(String.format("Total Due:       RM %.2f\n", totalDue));
            paymentReceipt.append(String.format("Amount Paid:     RM %.2f\n", amountEntered));
            paymentReceipt.append(String.format("Remaining Balance: RM %.2f\n\n", remainingBalance));

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

                clearScreen();

            } else {
                paymentReceipt.append("⚠ PARTIAL PAYMENT - Balance added to fines\n");
                paymentReceipt.append("Remaining RM ").append(String.format("%.2f", remainingBalance));
                paymentReceipt.append(" will be due on your next visit.\n");

                // Free up the spot even with partial payment (they leave)
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

                clearScreen();
            }

            paymentReceipt.append("\n").append("=".repeat(60));
            receiptArea.setText(paymentReceipt.toString());

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
            return 0.0;
        }
    }

    private void clearScreen() {
        plateField.setText("");
        amountPaidField.setText("0.00");
        receiptArea.setText("");
    }
}