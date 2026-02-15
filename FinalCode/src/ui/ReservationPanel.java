package ui;

import facade.ParkingSystemFacade;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class ReservationPanel extends JPanel {
    private ParkingSystemFacade facade;
    private JTextField licensePlateField;
    private JComboBox<String> spotCombo;
    private JSpinner startDateSpinner;
    private JSpinner endDateSpinner;
    private JButton createReservationBtn;
    private JTextArea reservationDisplayArea;

    public ReservationPanel(ParkingSystemFacade facade) {
        this.facade = facade;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initUI();
    }

    private void initUI() {
        // Top - Reservation Form
        JPanel formPanel = createReservationForm();
        add(formPanel, BorderLayout.NORTH);

        // Center - Display Area
        reservationDisplayArea = new JTextArea(15, 50);
        reservationDisplayArea.setEditable(false);
        reservationDisplayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(reservationDisplayArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Current Reservations"));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom - Buttons
        JPanel buttonPanel = new JPanel();
        JButton refreshBtn = new JButton("Refresh Available Spots");
        refreshBtn.addActionListener(e -> refreshAvailableSpots());
        buttonPanel.add(refreshBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        refreshAvailableSpots();
        displaySampleReservations();
    }

    private JPanel createReservationForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Create New Reservation"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // License Plate
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("License Plate:"), gbc);
        gbc.gridx = 1;
        licensePlateField = new JTextField(15);
        panel.add(licensePlateField, gbc);

        // Spot Selection
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Select Spot:"), gbc);
        gbc.gridx = 1;
        spotCombo = new JComboBox<>();
        spotCombo.setPreferredSize(new Dimension(200, 25));
        panel.add(spotCombo, gbc);

        // Start Time
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Start Time:"), gbc);
        gbc.gridx = 1;
        startDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor startEditor = new JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd HH:mm");
        startDateSpinner.setEditor(startEditor);
        startDateSpinner.setValue(new Date());
        panel.add(startDateSpinner, gbc);

        // End Time
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("End Time:"), gbc);
        gbc.gridx = 1;
        endDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor endEditor = new JSpinner.DateEditor(endDateSpinner, "yyyy-MM-dd HH:mm");
        endDateSpinner.setEditor(endEditor);
        // Set end time to 2 hours from now by default
        endDateSpinner.setValue(new Date(System.currentTimeMillis() + 2 * 60 * 60 * 1000));
        panel.add(endDateSpinner, gbc);

        // Create Button
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        createReservationBtn = new JButton("Create Reservation");
        createReservationBtn.addActionListener(e -> createReservation());
        panel.add(createReservationBtn, gbc);

        return panel;
    }

    private void refreshAvailableSpots() {
        try {
            LocalDateTime start = convertToLocalDateTime(startDateSpinner.getValue());
            LocalDateTime end = convertToLocalDateTime(endDateSpinner.getValue());

            if (start.isAfter(end) || start.equals(end)) {
                JOptionPane.showMessageDialog(this,
                        "End time must be after start time",
                        "Invalid Time",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<String> availableSpots = facade.getAvailableReservationSpots(start, end);
            spotCombo.removeAllItems();
            for (String spot : availableSpots) {
                spotCombo.addItem(spot);
            }

            if (availableSpots.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No reserved spots available for this time period",
                        "No Spots",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createReservation() {
        try {
            String licensePlate = licensePlateField.getText().trim().toUpperCase();
            if (licensePlate.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter license plate",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String selectedSpot = (String) spotCombo.getSelectedItem();
            if (selectedSpot == null) {
                JOptionPane.showMessageDialog(this,
                        "Please select a spot",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String spotId = selectedSpot.split(" - ")[0];
            LocalDateTime start = convertToLocalDateTime(startDateSpinner.getValue());
            LocalDateTime end = convertToLocalDateTime(endDateSpinner.getValue());

            facade.createReservation(licensePlate, spotId, start, end);

            // Clear form
            licensePlateField.setText("");
            spotCombo.removeAllItems();
            startDateSpinner.setValue(new Date());
            endDateSpinner.setValue(new Date(System.currentTimeMillis() + 2 * 60 * 60 * 1000));

            // Show success
            String message = "✓ Reservation created for " + licensePlate + " at " + spotId +
                    "\nFrom: " + start + "\nTo: " + end;
            reservationDisplayArea.append(message + "\n");
            JOptionPane.showMessageDialog(this,
                    message,
                    "Reservation Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // Refresh available spots
            refreshAvailableSpots();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error creating reservation: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displaySampleReservations() {
        reservationDisplayArea.setText("=== RESERVATION SYSTEM ===\n");
        reservationDisplayArea.append("Create reservations for Reserved spots (F3-R3-S9, F3-R3-S10)\n");
        reservationDisplayArea.append("Vehicles without reservations in Reserved spots will be fined RM50\n\n");
        reservationDisplayArea.append("Sample format: F3-R3-S9 - Reserved (RM 10.0/hr)\n\n");
    }

    private LocalDateTime convertToLocalDateTime(Object date) {
        Date utilDate = (Date) date;
        return utilDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}