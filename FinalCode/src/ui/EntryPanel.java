package ui;

import facade.ParkingSystemFacade;
import model.*;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class EntryPanel extends JPanel {
    private ParkingSystemFacade facade;
    private JComboBox<String> vehicleTypeCombo;
    private JTextField licensePlateField;
    private JCheckBox handicappedCardCheck;
    private JList<String> availableSpotsList;
    private DefaultListModel<String> spotListModel;
    private JTextArea ticketDisplayArea;
    private JButton parkButton;
    private JButton refreshSpotsButton;

    public EntryPanel(ParkingSystemFacade facade) {
        this.facade = facade;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel entryFormPanel = createEntryFormPanel();
        JPanel spotsPanel = createSpotsPanel();
        JPanel ticketPanel = createTicketPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, entryFormPanel, spotsPanel);
        splitPane.setResizeWeight(0.4);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane, ticketPanel);
        mainSplitPane.setResizeWeight(0.6);

        add(mainSplitPane, BorderLayout.CENTER);
    }

    private JPanel createEntryFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vehicle Entry"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("License Plate:"), gbc);

        gbc.gridx = 1;
        licensePlateField = new JTextField(15);
        panel.add(licensePlateField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Vehicle Type:"), gbc);

        gbc.gridx = 1;
        String[] types = {"CAR", "MOTORCYCLE", "SUV", "HANDICAPPED"};
        vehicleTypeCombo = new JComboBox<>(types);
        vehicleTypeCombo.addActionListener(e -> {
            boolean isHandicapped = vehicleTypeCombo.getSelectedItem().equals("HANDICAPPED");
            handicappedCardCheck.setEnabled(isHandicapped);
        });
        panel.add(vehicleTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Has Card:"), gbc);

        gbc.gridx = 1;
        handicappedCardCheck = new JCheckBox("Handicapped Card Holder");
        handicappedCardCheck.setEnabled(false);
        panel.add(handicappedCardCheck, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        refreshSpotsButton = new JButton("Find Available Spots");
        refreshSpotsButton.addActionListener(e -> refreshAvailableSpots());
        panel.add(refreshSpotsButton, gbc);

        gbc.gridy = 4;
        parkButton = new JButton("Park Vehicle");
        parkButton.addActionListener(e -> parkVehicle());
        parkButton.setEnabled(false);
        panel.add(parkButton, gbc);

        return panel;
    }

    private JPanel createSpotsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Available Parking Spots"));

        spotListModel = new DefaultListModel<>();
        availableSpotsList = new JList<>(spotListModel);
        availableSpotsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableSpotsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                parkButton.setEnabled(availableSpotsList.getSelectedIndex() != -1);
            }
        });

        JScrollPane scrollPane = new JScrollPane(availableSpotsList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTicketPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Generated Ticket"));

        ticketDisplayArea = new JTextArea();
        ticketDisplayArea.setEditable(false);
        ticketDisplayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ticketDisplayArea.setBackground(new Color(240, 240, 240));

        JScrollPane scrollPane = new JScrollPane(ticketDisplayArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // NEW: Method to check if vehicle is already parked
    private boolean isVehicleAlreadyParked(String licensePlate) {
        try {
            Ticket existingTicket = facade.findActiveTicket(licensePlate);
            return existingTicket != null;
        } catch (SQLException e) {
            return false;
        }
    }

    private void refreshAvailableSpots() {
        String licensePlate = licensePlateField.getText().trim().toUpperCase();
        if (licensePlate.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter license plate number",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String vehicleType = (String) vehicleTypeCombo.getSelectedItem();
        boolean hasCard = handicappedCardCheck.isSelected();

        Vehicle vehicle;
        switch (vehicleType) {
            case "CAR": vehicle = new Car(licensePlate); break;
            case "MOTORCYCLE": vehicle = new Motorcycle(licensePlate); break;
            case "SUV": vehicle = new SUV(licensePlate); break;
            case "HANDICAPPED":
                vehicle = new HandicappedVehicle(licensePlate, hasCard);
                break;
            default: vehicle = null;
        }

        List<ParkingSpot> availableSpots = facade.getAvailableSpotsForVehicle(vehicle);
        spotListModel.clear();

        for (ParkingSpot spot : availableSpots) {
            if (vehicle instanceof HandicappedVehicle hv) {
                hasCard = hv.hasHandicappedCard();

                if (hasCard && spot.getType().equalsIgnoreCase("Handicapped")) {
                    spotListModel.addElement("★ " + spot.getSpotId() + " - " + spot.getType() + " (FREE)");
                } else if (hasCard) {
                    spotListModel.addElement(spot.getSpotId() + " - " + spot.getType() + " (RM 2.00/hr)");
                } else {
                    spotListModel.addElement(spot.getSpotId() + " - " + spot.getType() + " (RM " + spot.getHourlyRate() + "/hr)");
                }
            } else {
                spotListModel.addElement(spot.getSpotId() + " - " + spot.getType() + " (RM " + spot.getHourlyRate() + "/hr)");
            }
        }

        if (spotListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No available spots for this vehicle type",
                    "No Spots",
                    JOptionPane.WARNING_MESSAGE);
            parkButton.setEnabled(false);
        }
    }

    private void parkVehicle() {
        try {
            String licensePlate = licensePlateField.getText().trim().toUpperCase();

            // NEW: Check if vehicle is already parked
            if (isVehicleAlreadyParked(licensePlate)) {
                JOptionPane.showMessageDialog(this,
                        "Vehicle with license plate " + licensePlate + " is already parked!",
                        "Duplicate Vehicle",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String selectedSpotStr = availableSpotsList.getSelectedValue();
            if (selectedSpotStr == null) return;

            // Fix: Remove any special characters and extract just the spot ID
            String spotId = selectedSpotStr.split(" - ")[0];
            // Remove the ★ if present
            spotId = spotId.replace("★ ", "").trim();

            System.out.println("Selected spot string: " + selectedSpotStr);
            System.out.println("Extracted spotId: " + spotId); // Debug

            String vehicleType = (String) vehicleTypeCombo.getSelectedItem();
            boolean hasCard = handicappedCardCheck.isSelected();

            facade.parkVehicle(licensePlate, vehicleType, hasCard, spotId);

            Ticket ticket = facade.findActiveTicket(licensePlate);
            ticketDisplayArea.setText(ticket.toString());

            // Clear form
            licensePlateField.setText("");
            vehicleTypeCombo.setSelectedIndex(0);
            handicappedCardCheck.setSelected(false);
            handicappedCardCheck.setEnabled(false);
            spotListModel.clear();
            parkButton.setEnabled(false);

            JOptionPane.showMessageDialog(this,
                    "Vehicle parked successfully!\nTicket generated.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}