package dao;
import database.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;

public class PaymentDAO {
    public void savePayment(String ticketId, String plate, double amount, String method) throws SQLException {
        String sql = "INSERT INTO payments (payment_id, ticket_id, license_plate, amount_paid, payment_method, payment_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "PAY-" + System.currentTimeMillis());
            pstmt.setString(2, ticketId);
            pstmt.setString(3, plate);
            pstmt.setDouble(4, amount);
            pstmt.setString(5, method);
            pstmt.setString(6, LocalDateTime.now().toString());
            pstmt.executeUpdate();
        }
    }

    public double getUnpaidFines(String plate) throws SQLException {
        String sql = "SELECT SUM(fine_amount) FROM fines WHERE license_plate = ? AND is_paid = 0";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plate);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }
}