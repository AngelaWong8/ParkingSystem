package dao;

import database.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public double getTotalRevenueToday() throws SQLException {
        String sql = "SELECT SUM(amount_paid) FROM payments WHERE date(payment_time) = date('now')";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            // Table doesn't exist yet (Person C not done)
            return 0;
        }
        return 0;
    }

    public double getTotalFinesToday() throws SQLException {
        String sql = "SELECT SUM(fine_amount) FROM fines WHERE is_paid = 1";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            // Table doesn't exist yet (Person C not done)
            return 0;
        }
        return 0;
    }

    public List<Object[]> getHourlyOccupancy() throws SQLException {
        List<Object[]> data = new ArrayList<>();
        String sql = "SELECT strftime('%H', entry_time) as hour, COUNT(*) as count " +
                "FROM tickets GROUP BY hour ORDER BY hour";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                data.add(new Object[]{rs.getString("hour"), rs.getInt("count")});
            }
        }
        return data;
    }

    public void updateDailyRevenue() throws SQLException {
        String sql = "INSERT OR REPLACE INTO daily_revenue (date, total_payments, total_fines, total_vehicles) " +
                "VALUES (date('now'), ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, getTotalRevenueToday());
            pstmt.setDouble(2, 0); // getTotalFinesToday() - commented until Person C
            pstmt.setInt(3, getCurrentVehicleCount());
            pstmt.executeUpdate();
        }
    }

    private int getCurrentVehicleCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM tickets";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}