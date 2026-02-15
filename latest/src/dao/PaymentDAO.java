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
        String sql = "SELECT SUM(fine_amount - paid_amount) FROM fines WHERE license_plate = ? AND is_paid = 0";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plate);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    public double getTotalPaidForTicket(String ticketId) throws SQLException {
        String sql = "SELECT SUM(amount_paid) FROM payments WHERE ticket_id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    public void createFine(String ticketId, String plate, double amount, long overstayMinutes, String method) throws SQLException {
        String sql = "INSERT INTO fines (fine_id, ticket_id, license_plate, fine_amount, original_amount, paid_amount, overstay_minutes, calculation_method, is_paid, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "FINE-" + System.currentTimeMillis());
            pstmt.setString(2, ticketId);
            pstmt.setString(3, plate);
            pstmt.setDouble(4, amount);
            pstmt.setDouble(5, amount);
            pstmt.setDouble(6, 0);
            pstmt.setLong(7, overstayMinutes);
            pstmt.setString(8, method);
            pstmt.setInt(9, 0);
            pstmt.setString(10, LocalDateTime.now().toString());
            pstmt.executeUpdate();
        }
    }

    public void updateFinePayment(String plate, double amountPaid) throws SQLException {
        // Get oldest unpaid fine first (FIFO approach)
        String sql = "SELECT fine_id, fine_amount, paid_amount FROM fines WHERE license_plate = ? AND is_paid = 0 ORDER BY created_date ASC";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plate);
            ResultSet rs = pstmt.executeQuery();

            double remainingToAllocate = amountPaid;

            while (rs.next() && remainingToAllocate > 0) {
                String fineId = rs.getString("fine_id");
                double fineAmount = rs.getDouble("fine_amount");
                double alreadyPaid = rs.getDouble("paid_amount");
                double outstanding = fineAmount - alreadyPaid;

                double paymentForThisFine = Math.min(remainingToAllocate, outstanding);
                remainingToAllocate -= paymentForThisFine;

                // Update this fine
                String updateSql = "UPDATE fines SET paid_amount = paid_amount + ? WHERE fine_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setDouble(1, paymentForThisFine);
                    updateStmt.setString(2, fineId);
                    updateStmt.executeUpdate();
                }

                // If fully paid, mark as paid
                if (outstanding - paymentForThisFine <= 0.01) { // Small epsilon for floating point
                    String markPaidSql = "UPDATE fines SET is_paid = 1 WHERE fine_id = ?";
                    try (PreparedStatement markStmt = conn.prepareStatement(markPaidSql)) {
                        markStmt.setString(1, fineId);
                        markStmt.executeUpdate();
                    }
                }
            }
        }
    }
}
