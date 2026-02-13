package ui;

import dao.*;
import model.*;
import logic.PaymentLogic;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;

public class ExitPanel extends JPanel {
    private JTextField plateInput = new JTextField(10);
    private JTextArea receiptArea = new JTextArea(10, 25);
    private JButton exitBtn = new JButton("Process Exit");
    private PaymentLogic logic = new PaymentLogic();

    public ExitPanel() {
        setLayout(new BorderLayout(10, 10));
        
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("License Plate:"));
        inputPanel.add(plateInput);
        inputPanel.add(exitBtn);
        
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(receiptArea), BorderLayout.CENTER);

        exitBtn.addActionListener(e -> handleExit());
    }

    private void handleExit() {
        String plate = plateInput.getText().toUpperCase();
        try {
            TicketDAO tDao = new TicketDAO();
            Ticket ticket = tDao.findActiveTicket(plate);

            if (ticket == null) {
                JOptionPane.showMessageDialog(this, "No active parking found for " + plate);
                return;
            }

            // Calculation (Using RM 5 as default rate)
            double parkingFee = logic.calculateFee(ticket.getEntryTime(), 5.0);
            double fine = logic.calculateFine(ticket.getEntryTime(), "A"); // Default to Scheme A
            double total = parkingFee + fine;

            // Display Receipt
            receiptArea.setText("--- PARKING RECEIPT ---\n" +
                               "Plate: " + plate + "\n" +
                               "In: " + ticket.getEntryTime().toString() + "\n" +
                               "Parking Fee: RM " + parkingFee + "\n" +
                               "Fines: RM " + fine + "\n" +
                               "-----------------------\n" +
                               "TOTAL PAID: RM " + total);

            // Database Updates
            new ExitDAO().processPayment(plate, total, "CASH");
            tDao.removeTicket(plate);
            
            JOptionPane.showMessageDialog(this, "Payment Successful. Gate Opening...");
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}