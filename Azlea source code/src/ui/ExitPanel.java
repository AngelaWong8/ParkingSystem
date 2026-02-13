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
    private JTextField plateField;
    private JTextArea receiptArea;
    private FineStrategy fineStrategy = new FixedFineStrategy(); // Admin can toggle this

    public ExitPanel(ParkingSystemFacade facade) {
        this.facade = facade;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Input Top
        JPanel north = new JPanel();
        plateField = new JTextField(12);
        JButton btnCheck = new JButton("Calculate Fees");
        north.add(new JLabel("License Plate: "));
        north.add(plateField);
        north.add(btnCheck);

        receiptArea = new JTextArea();
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        receiptArea.setEditable(false);

        btnCheck.addActionListener(e -> calculateExit());

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(receiptArea), BorderLayout.CENTER);
    }

    private void calculateExit() {
        try {
            String plate = plateField.getText().trim();
            Ticket ticket = facade.findActiveTicket(plate);
            if (ticket == null) {
                JOptionPane.showMessageDialog(this, "No active parking record.");
                return;
            }

            // 1. Duration & Ceiling Rounding
            LocalDateTime now = LocalDateTime.now();
            long mins = Duration.between(ticket.getEntryTime(), now).toMinutes();
            long hours = (long) Math.ceil(mins / 60.0);
            if (hours == 0) hours = 1;

            // 2. Fees (Fetch spot rate via existing DAO)
            ParkingSpot spot = new ParkingSpotDAO().getSpotBySpotId(ticket.getSpotId());
            double rate = (spot != null) ? spot.getHourlyRate() : 5.0;
            double baseFee = hours * rate;

            // 3. Unpaid Fines check
            double oldFines = new PaymentDAO().getUnpaidFines(plate);
            double total = baseFee + oldFines;

            // Display Receipt
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

            int pay = JOptionPane.showConfirmDialog(this, "Process Payment of RM " + total + "?", "Payment", JOptionPane.YES_NO_OPTION);
            if (pay == JOptionPane.YES_OPTION) {
                finalizeExit(ticket, plate, total, spot);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void finalizeExit(Ticket t, String plate, double amt, ParkingSpot s) throws Exception {
        // Record payment
        new PaymentDAO().savePayment(t.getTicketId(), plate, amt, "Cash/Card");

        // Remove active ticket
        new TicketDAO().removeTicket(plate);

        // Set spot to available (Person A's DAO)
        if (s != null) {
            new ParkingSpotDAO().updateSpotStatus(s.getDbId(), false);
        }

        receiptArea.setText("PAYMENT SUCCESSFUL\nGate Opening...");
        plateField.setText("");
    }
}