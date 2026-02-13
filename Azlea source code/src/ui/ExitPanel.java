package ui;

import facade.ParkingSystemFacade;
import model.*;
import dao.*;
import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class ExitPanel extends JPanel {
    private ParkingSystemFacade facade;
    private JTextField plateField = new JTextField(15);
    private JTextArea receiptArea = new JTextArea(15, 30);
    private FineStrategy fineStrategy = new FixedFineStrategy();

    // Set this to TRUE to test fines without waiting hours
    private boolean DEBUG_MODE = false;

    public ExitPanel(ParkingSystemFacade facade) {
        this.facade = facade;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Top Input Area ---
        JPanel inputPanel = new JPanel();
        JButton checkBtn = new JButton("Calculate Fees");
        JButton payBtn = new JButton("Confirm Payment & Exit");
        payBtn.setBackground(new Color(34, 139, 34)); // Green button
        payBtn.setForeground(Color.WHITE);

        inputPanel.add(new JLabel("License Plate:"));
        inputPanel.add(plateField);
        inputPanel.add(checkBtn);
        inputPanel.add(payBtn);

        // --- Receipt Area ---
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        receiptArea.setEditable(false);
        receiptArea.setBackground(new Color(245, 245, 245));

        // --- Button Actions ---
        checkBtn.addActionListener(e -> calculateExit());
        payBtn.addActionListener(e -> processPayment());

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(receiptArea), BorderLayout.CENTER);
    }

    private void calculateExit() {
        try {
            String plate = plateField.getText().trim();
            Ticket ticket = facade.findActiveTicket(plate);

            if (ticket == null) {
                receiptArea.setText("NO ACTIVE TICKET FOUND FOR: " + plate);
                return;
            }

            // 1. Time Logic
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime entry = ticket.getEntryTime();
            if (DEBUG_MODE) entry = now.minusMinutes(135); // Simulating 2hr 15min stay

            long mins = Duration.between(entry, now).toMinutes();
            long hours = (long) Math.ceil(mins / 60.0);
            if (hours == 0) hours = 1;

            // 2. Pricing Logic
            ParkingSpot spot = new ParkingSpotDAO().getSpotBySpotId(ticket.getSpotId());
            double rate = (spot != null) ? spot.getHourlyRate() : 5.0;
            double baseFee = hours * rate;

            // 3. Fine Logic (If stay > 2 hours)
            double oldFines = (mins > 120) ? fineStrategy.calculateFine(mins - 120) : 0.0;
            double total = baseFee + oldFines;

            // --- YOUR EXACT LAYOUT ---
            receiptArea.setText("======== EXIT RECEIPT ========\n");
            receiptArea.append("Plate: " + plate + "\n");
            receiptArea.append("Spot:  " + ticket.getSpotId() + "\n");
            receiptArea.append("Stay:  " + mins + " mins (" + hours + " hrs)\n");
            receiptArea.append("Rate:  RM " + rate + "/hr\n");
            receiptArea.append("------------------------------\n");
            receiptArea.append("Parking Fee:   RM " + String.format("%.2f", baseFee) + "\n");
            receiptArea.append("Unpaid Fines:  RM " + String.format("%.2f", oldFines) + "\n");
            receiptArea.append("TOTAL DUE:     RM " + String.format("%.2f", total) + "\n");
            receiptArea.append("==============================");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void processPayment() {
        if (receiptArea.getText().isEmpty() || receiptArea.getText().contains("NO ACTIVE TICKET")) {
            JOptionPane.showMessageDialog(this, "Please calculate fees first!");
            return;
        }

        try {
            String plate = plateField.getText().trim();
            Ticket ticket = facade.findActiveTicket(plate);

            // Logic to finalize things:
            // 1. Mark Spot as Available (Person A's logic)
            ParkingSpot spot = new ParkingSpotDAO().getSpotBySpotId(ticket.getSpotId());
            if (spot != null) {
                new ParkingSpotDAO().updateSpotStatus(spot.getDbId(), false);
            }

            // 2. Delete the ticket (Person B's logic)
            new TicketDAO().removeTicket(plate);

            // 3. Show Success
            JOptionPane.showMessageDialog(this, "Payment Received! Gate Opening for " + plate);

            // Clear screen
            plateField.setText("");
            receiptArea.setText("GATE OPENED - HAVE A NICE DAY!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Payment Failed: " + ex.getMessage());
        }
    }
}