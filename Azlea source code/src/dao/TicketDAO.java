package dao;

import model.Ticket;
import database.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public void saveTicket(Ticket ticket) throws SQLException {
        String sql = "INSERT INTO tickets (ticket_id, license_plate, spot_id, entry_time) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ticket.getTicketId());
            pstmt.setString(2, ticket.getLicensePlate());
            pstmt.setString(3, ticket.getSpotId());
            pstmt.setString(4, ticket.getEntryTime().format(FORMATTER));
            pstmt.executeUpdate();
        }
    }

    public Ticket findActiveTicket(String licensePlate) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE license_plate = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, licensePlate);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 1. Create the ticket object
                    Ticket ticket = new Ticket(
                            rs.getString("license_plate"),
                            rs.getString("spot_id")
                    );

                    // 2. Load the ACTUAL entry time from the database
                    String entryTimeStr = rs.getString("entry_time");
                    if (entryTimeStr != null) {
                        // Convert the text back into a date/time object
                        LocalDateTime dbTime = LocalDateTime.parse(entryTimeStr, FORMATTER);
                        // Overwrite the "now" time with the "database" time
                        ticket.setEntryTime(dbTime);
                    }

                    return ticket;
                }
            }
        }
        return null;
    }

    public void removeTicket(String licensePlate) throws SQLException {
        String sql = "DELETE FROM tickets WHERE license_plate = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, licensePlate);
            pstmt.executeUpdate();
        }
    }

    public List<Ticket> getAllActiveTickets() throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tickets.add(new Ticket(
                        rs.getString("license_plate"),
                        rs.getString("spot_id")
                ));
            }
        }
        return tickets;
    }
}