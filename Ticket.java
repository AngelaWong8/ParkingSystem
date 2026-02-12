package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Ticket {
    private String ticketId;
    private String licensePlate;
    private String spotId;
    private LocalDateTime entryTime;
    
    public Ticket(String licensePlate, String spotId) {
        this.licensePlate = licensePlate;
        this.spotId = spotId;
        this.entryTime = LocalDateTime.now();
        this.ticketId = generateTicketId();
    }
    
    private String generateTicketId() {
        // Format: T-PLATE-TIMESTAMP
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "T-" + licensePlate + "-" + entryTime.format(formatter);
    }
    
    // Getters
    public String getTicketId() { return ticketId; }
    public String getLicensePlate() { return licensePlate; }
    public String getSpotId() { return spotId; }
    public LocalDateTime getEntryTime() { return entryTime; }
    
    @Override
    public String toString() {
        return "Ticket: " + ticketId + "\n" +
               "Vehicle: " + licensePlate + "\n" +
               "Spot: " + spotId + "\n" +
               "Entry Time: " + entryTime;
    }
}