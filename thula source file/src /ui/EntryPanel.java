package ui;

import model.*;
import dao.VehicleDAO;
import dao.TicketDAO;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EntryPanel extends JPanel {
    private JComboBox<String> vehicleTypeCombo;
    private JTextField licensePlateField;
    private JCheckBox handicappedCardCheck;
    private JList<String> availableSpotsList;
    private DefaultListModel<String> spotListModel;
    private JTextArea ticketDisplayArea;
    private JButton parkButton;
    private JButton refreshSpotsButton;
    
    private VehicleDAO vehicleDAO;
    private TicketDAO ticketDAO;
    
    // This would normally come from Person A's module
    // For now, we'll simulate with some hardcoded spots
    private List<ParkingSpot> availableSpots;
    
    public EntryPanel() {
        vehicleDAO = new VehicleDAO();
        ticketDAO = new TicketDAO();
        availableSpots = new ArrayList<>();
        
        initializeSampleSpots();
        initUI();
    }
    
    private void initializeSampleSpots() {
        // TEMPORARY - Person A will provide actual parking spots
        availableSpots.add(new ParkingSpot("F1-R1-S1", "COMPACT", 2.0));
        availableSpots.add(new ParkingSpot("F1-R1-S2", "COMPACT", 2.0));
        availableSpots.add(new ParkingSpot("F1-R2-S1", "REGULAR", 5.0));
        availableSpots.add(new ParkingSpot("F1-R2-S2", "REGULAR", 5.0));
        availableSpots.add(new ParkingSpot("F2-R1-S1", "HANDICAPPED", 2.0));
        availableSpots.add(new ParkingSpot("F2-R1-S2", "RESERVED", 10.0));
    }
    
    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Left Panel - Vehicle Entry Form
        JPanel entryFormPanel = createEntryFormPanel();
        
        // Center Panel - Available Spots
        JPanel spotsPanel = createSpotsPanel();
        
        // Right Panel - Ticket Display
        JPanel ticketPanel = createTicketPanel();
        
        // Split pane to organize
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
        
        // License Plate
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("License Plate:"), gbc);
        
        gbc.gridx = 1;
        licensePlateField = new JTextField(15);
        panel.add(licensePlateField, gbc);
        
        // Vehicle Type
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
        
        // Handicapped Card (only enabled for handicapped vehicles)
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Has Card:"), gbc);
        
        gbc.gridx = 1;
        handicappedCardCheck = new JCheckBox("Handicapped Card Holder");
        handicappedCardCheck.setEnabled(false);
        panel.add(handicappedCardCheck, gbc);
        
        // Buttons
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
    
            private void refreshAvailableSpots() {
                String licensePlate = licensePlateField.getText().trim();
                if (licensePlate.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "Please enter license plate number", 
                        "Validation Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String vehicleType = (String) vehicleTypeCombo.getSelectedItem();
                boolean hasCard = handicappedCardCheck.isSelected();
                
                // Create vehicle object to check compatibility
                Vehicle vehicle;
                switch (vehicleType) {
                    case "CAR": vehicle = new Car(licensePlate); break;
                    case "MOTORCYCLE": vehicle = new Motorcycle(licensePlate); break;
                    case "SUV": vehicle = new SUV(licensePlate); break;
                    case "HANDICAPPED": 
                        if (!hasCard) {
                            JOptionPane.showMessageDialog(this,
                                "Handicapped vehicles MUST have a handicapped card!",
                                "Validation Error",
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        vehicle = new HandicappedVehicle(licensePlate, true); 
                        break;
                    default: vehicle = null;
                }
                
                // Filter available spots based on REALISTIC vehicle compatibility
                spotListModel.clear();
                for (ParkingSpot spot : availableSpots) {
                    if (spot.isAvailable() && vehicle.canParkIn(spot.getSpotType())) {
                        // For handicapped vehicles, prioritize handicapped spots
                        if (vehicle instanceof HandicappedVehicle) {
                            if (spot.getSpotType().equalsIgnoreCase("HANDICAPPED")) {
                                spotListModel.addElement("★ " + spot.toString() + " (PRIORITY)");
                            } else {
                                spotListModel.addElement(spot.toString() + " (alternative)");
                            }
                        } else {
                            spotListModel.addElement(spot.toString());
                        }
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
            // Get selected spot
            String selectedSpotStr = availableSpotsList.getSelectedValue();
            if (selectedSpotStr == null) return;
            
            String spotId = selectedSpotStr.split(" - ")[0];
            
            // Get vehicle details
            String licensePlate = licensePlateField.getText().trim();
            String vehicleType = (String) vehicleTypeCombo.getSelectedItem();
            boolean hasCard = handicappedCardCheck.isSelected();
            
            // Create vehicle
            Vehicle vehicle;
            switch (vehicleType) {
                case "CAR": vehicle = new Car(licensePlate); break;
                case "MOTORCYCLE": vehicle = new Motorcycle(licensePlate); break;
                case "SUV": vehicle = new SUV(licensePlate); break;
                case "HANDICAPPED": vehicle = new HandicappedVehicle(licensePlate, hasCard); break;
                default: throw new IllegalArgumentException("Invalid vehicle type");
            }
            
            // Set entry time
            vehicle.setEntryTime(java.time.LocalDateTime.now());
            
            // Save vehicle to database
            vehicleDAO.saveVehicle(vehicle);
            
            // Create and save ticket
            Ticket ticket = new Ticket(licensePlate, spotId);
            ticketDAO.saveTicket(ticket);
            
            // Mark spot as occupied (in real system, this would call Person A's module)
            for (ParkingSpot spot : availableSpots) {
                if (spot.getSpotId().equals(spotId)) {
                    spot.setAvailable(false);
                    break;
                }
            }
            
            // Display ticket
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
                
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Database error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
