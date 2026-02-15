package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Ticket {
    private String ticketId;
    private String licensePlate;
    private String spotId;
    private LocalDateTime entryTime;

    // Constructor for NEW tickets (entry time = now)
    public Ticket(String licensePlate, String spotId) {
        this.licensePlate = licensePlate.toUpperCase(); // Store as uppercase
        this.spotId = spotId;
        this.entryTime = LocalDateTime.now();
        this.ticketId = generateTicketId();
    }

    // Constructor for EXISTING tickets (with specific entry time)
    public Ticket(String ticketId, String licensePlate, String spotId, LocalDateTime entryTime) {
        this.ticketId = ticketId;
        this.licensePlate = licensePlate.toUpperCase(); // Store as uppercase
        this.spotId = spotId;
        this.entryTime = entryTime;
    }

    private String generateTicketId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "T-" + licensePlate + "-" + entryTime.format(formatter);
    }

    public String getTicketId() { return ticketId; }
    public String getLicensePlate() { return licensePlate; }
    public String getSpotId() { return spotId; }
    public LocalDateTime getEntryTime() { return entryTime; }

    @Override
    public String toString() {
        return "Ticket: " + ticketId + "\n" +
                "Vehicle: " + licensePlate + "\n" +
                "Spot: " + spotId + "\n" +
                "Entry Time: " + entryTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}