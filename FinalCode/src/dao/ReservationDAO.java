package dao;

import database.DatabaseConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    public boolean hasActiveReservation(String licensePlate, String spotId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE license_plate = ? AND spot_id = ? AND end_time > ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, licensePlate);
            pstmt.setString(2, spotId);
            pstmt.setString(3, LocalDateTime.now().toString());

            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            return false;
        }
    }

    // ADD THIS MISSING METHOD
    public List<String> getReservedSpotsInTimeRange(LocalDateTime start, LocalDateTime end) {
        List<String> reservedSpots = new ArrayList<>();
        String sql = "SELECT spot_id FROM reservations WHERE " +
                "(start_time <= ? AND end_time >= ?) OR " + // Overlaps
                "(start_time >= ? AND start_time <= ?)";    // Starts within

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, end.toString());
            pstmt.setString(2, start.toString());
            pstmt.setString(3, start.toString());
            pstmt.setString(4, end.toString());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservedSpots.add(rs.getString("spot_id"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservedSpots;
    }

    public void createReservation(String licensePlate, String spotId, LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "INSERT INTO reservations (reservation_id, license_plate, spot_id, start_time, end_time) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "RES-" + System.currentTimeMillis());
            pstmt.setString(2, licensePlate);
            pstmt.setString(3, spotId);
            pstmt.setString(4, start.toString());
            pstmt.setString(5, end.toString());
            pstmt.executeUpdate();
        }
    }
}