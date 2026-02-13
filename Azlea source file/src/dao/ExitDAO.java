package dao;

import database.DatabaseConnection;
import java.sql.*;

public class ExitDAO {
    
    public void processPayment(String plate, double amount, String method) throws SQLException {
        String sql = "INSERT INTO transactions (license_plate, amount_paid, payment_method) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, plate);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, method);
            pstmt.executeUpdate();
        }
    }

    public void clearAccountFine(String plate) throws SQLException {
        String sql = "DELETE FROM unpaid_fines WHERE license_plate = ?";
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, plate);
            pstmt.executeUpdate();
        }
    }
}