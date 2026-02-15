package dao;

import database.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportDAO {

    private static final Logger LOGGER = Logger.getLogger(ReportDAO.class.getName());

    public double getTotalRevenueToday() throws SQLException {
        // Get today's date
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Sum ONLY parking fee payments (not fines)
        String sql = "SELECT SUM(amount_paid) FROM payments WHERE date(payment_time) = ? " +
                "AND (payment_method = 'Cash' OR payment_method = 'Card' OR payment_method = 'Cash (FREE)')";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, today);
            ResultSet rs = pstmt.executeQuery();

            double result = rs.next() ? rs.getDouble(1) : 0.0;
            LOGGER.info("Total parking revenue today: RM " + result);
            return result;
        }
    }

    public double getTotalFinesToday() throws SQLException {
        // Get today's date
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Sum ONLY fine payments
        String sql = "SELECT SUM(amount_paid) FROM payments WHERE date(payment_time) = ? " +
                "AND payment_method LIKE '%FINE%'";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, today);
            ResultSet rs = pstmt.executeQuery();

            double result = rs.next() ? rs.getDouble(1) : 0.0;
            LOGGER.info("Total fines paid today: RM " + result);
            return result;
        }
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
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error getting hourly occupancy", e);
        }
        return data;
    }

    public void updateDailyRevenue() throws SQLException {
        String sql = "INSERT OR REPLACE INTO daily_revenue (date, total_payments, total_fines, total_vehicles) " +
                "VALUES (date('now'), ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, getTotalRevenueToday());
            pstmt.setDouble(2, getTotalFinesToday());
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

    public double getReservationFinesToday() throws SQLException {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String sql = "SELECT SUM(amount_paid) FROM payments WHERE date(payment_time) = ? " +
                "AND payment_method LIKE '%RESERVATION%'";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    public double getOverstayFinesToday() throws SQLException {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String sql = "SELECT SUM(amount_paid) FROM payments WHERE date(payment_time) = ? " +
                "AND payment_method LIKE '%OVERSTAY%'";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    public double getParkingRevenueToday() throws SQLException {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String sql = "SELECT SUM(amount_paid) FROM payments WHERE date(payment_time) = ? " +
                "AND (payment_method = 'Cash' OR payment_method = 'Card')";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }
}
