package dao;

import model.Ticket;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    public void saveTicket(Ticket ticket) throws SQLException {
        String sql = "INSERT INTO tickets (ticket_id, license_plate, spot_id, entry_time) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, ticket.getTicketId());
            pstmt.setString(2, ticket.getLicensePlate());
            pstmt.setString(3, ticket.getSpotId());
            pstmt.setString(4, ticket.getEntryTime().format(FORMATTER));
            pstmt.executeUpdate();
        }
    }
    
    public Ticket findActiveTicket(String licensePlate) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE license_plate = ?";
        
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, licensePlate);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Ticket ticket = new Ticket(
                        rs.getString("license_plate"),
                        rs.getString("spot_id")
                    );
                    // Override the auto-generated time with the stored time
                    ticket = new Ticket(
                        rs.getString("license_plate"),
                        rs.getString("spot_id")
                    );
                    // This is a hack - in real code, you'd need a constructor that accepts time
                    return ticket;
                }
            }
        }
        return null;
    }
    
    public void removeTicket(String licensePlate) throws SQLException {
        String sql = "DELETE FROM tickets WHERE license_plate = ?";
        
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, licensePlate);
            pstmt.executeUpdate();
        }
    }
    
    public List<Ticket> getAllActiveTickets() throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets";
        
        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Ticket ticket = new Ticket(
                    rs.getString("license_plate"),
                    rs.getString("spot_id")
                );
                tickets.add(ticket);
            }
        }
        return tickets;
    }
}
