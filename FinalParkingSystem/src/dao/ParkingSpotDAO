package dao;

import database.DatabaseConnection;
import model.ParkingSpot;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParkingSpotDAO {

    public void createTable() {
        DatabaseConnection.createTables();
    }

    public void initializeSpotsIfEmpty() {
        String checkSql = "SELECT COUNT(*) FROM parking_spots";
        String insertSql = "INSERT INTO parking_spots(id, spot_id_str, type, floor_number, row_number, hourly_rate) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Generating 3 Floors x 3 Rows x 10 Spots...");

                PreparedStatement pstmt = conn.prepareStatement(insertSql);
                int globalId = 1;

                for (int f = 1; f <= 3; f++) {
                    for (int r = 1; r <= 3; r++) {
                        for (int s = 1; s <= 10; s++) {

                            String clientSpotId = "F" + f + "-R" + r + "-S" + s;

                            String type = "Regular";
                            double rate = 5.0;

                            if (r == 1 && s <= 5) {
                                type = "Compact";
                                rate = 2.0;
                            }
                            else if (r == 3 && s >= 9) {
                                type = "Reserved";
                                rate = 10.0;
                            }
                            else if (f == 1 && r == 1 && s >= 8) {
                                type = "Handicapped";
                                rate = 2.0;
                            }

                            pstmt.setInt(1, globalId++);
                            pstmt.setString(2, clientSpotId);
                            pstmt.setString(3, type);
                            pstmt.setInt(4, f);
                            pstmt.setInt(5, r);
                            pstmt.setDouble(6, rate);
                            pstmt.executeUpdate();
                        }
                    }
                }
                System.out.println("Database initialization complete.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<ParkingSpot> getAllSpots() {
        List<ParkingSpot> spots = new ArrayList<>();
        String sql = "SELECT * FROM parking_spots";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ParkingSpot spot = new ParkingSpot(
                        rs.getInt("id"),
                        rs.getString("spot_id_str"),
                        rs.getString("type"),
                        rs.getInt("floor_number"),
                        rs.getInt("row_number"),
                        rs.getDouble("hourly_rate")
                );
                spot.setOccupied(rs.getInt("is_occupied") == 1);
                spots.add(spot);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return spots;
    }

    public ParkingSpot getSpotBySpotId(String spotId) {
        String sql = "SELECT * FROM parking_spots WHERE spot_id_str = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, spotId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                ParkingSpot spot = new ParkingSpot(
                        rs.getInt("id"),
                        rs.getString("spot_id_str"),
                        rs.getString("type"),
                        rs.getInt("floor_number"),
                        rs.getInt("row_number"),
                        rs.getDouble("hourly_rate")
                );
                spot.setOccupied(rs.getInt("is_occupied") == 1);
                return spot;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public void updateSpotStatus(int id, boolean occupied) {
        String sql = "UPDATE parking_spots SET is_occupied = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, occupied ? 1 : 0);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();

            System.out.println("Spot " + id + " status updated in DB.");
        } catch (SQLException e) {
            System.out.println("Error updating status: " + e.getMessage());
        }
    }
}
