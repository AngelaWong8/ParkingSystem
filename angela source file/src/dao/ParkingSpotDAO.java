package dao;

import database.DatabaseConnection;
import model.ParkingSpot;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParkingSpotDAO {

    public void createTable() {
        // Added columns: spot_id_str, row_number, hourly_rate
        String sql = "CREATE TABLE IF NOT EXISTS parking_spots (" +
                "id INTEGER PRIMARY KEY, " +
                "spot_id_str TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "floor_number INTEGER, " +
                "row_number INTEGER, " +
                "hourly_rate REAL, " +
                "is_occupied INTEGER DEFAULT 0)";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void initializeSpotsIfEmpty() {
        String checkSql = "SELECT COUNT(*) FROM parking_spots";
        String insertSql = "INSERT INTO parking_spots(id, spot_id_str, type, floor_number, row_number, hourly_rate) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Generating 5 Floors x 3 Rows x 10 Spots...");

                PreparedStatement pstmt = conn.prepareStatement(insertSql);
                int globalId = 1;

                // 5 Floors
                for (int f = 1; f <= 5; f++) {
                    // 3 Rows per floor
                    for (int r = 1; r <= 3; r++) {
                        // 10 Spots per row
                        for (int s = 1; s <= 10; s++) {

                            // 1. Generate ID: "F1-R1-S1"
                            String clientSpotId = "F" + f + "-R" + r + "-S" + s;

                            // 2. Determine Type & Rate based on Row
                            String type = "Regular";
                            double rate = 5.0;

                            if (r == 1 && s <= 5) { type = "Compact"; rate = 2.0; } // Row 1 first half
                            else if (r == 3 && s >= 9) { type = "Reserved"; rate = 10.0; } // Row 3 end
                            else if (f == 1 && r == 1 && s >= 8) { type = "Handicapped"; rate = 2.0; } // Ground floor specific

                            // 3. Insert
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
                spots.add(new ParkingSpot(
                        rs.getInt("id"),
                        rs.getString("spot_id_str"), // Fetch the fancy ID
                        rs.getString("type"),
                        rs.getInt("floor_number"),
                        rs.getInt("row_number"),
                        rs.getDouble("hourly_rate")
                ));
                spots.get(spots.size()-1).setOccupied(rs.getInt("is_occupied") == 1);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return spots;
    }

    // Person A provides the "Update Tool" for the team
    public void updateSpotStatus(int id, boolean occupied) {
    String sql = "UPDATE parking_spots SET is_occupied = ? WHERE id = ?";
    
    try (Connection conn = DatabaseConnection.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
        pstmt.setInt(1, occupied ? 1 : 0); // 1 for occupied, 0 for empty
        pstmt.setInt(2, id);
        pstmt.executeUpdate();
        
        System.out.println("Spot " + id + " status updated in DB.");
    } catch (SQLException e) {
        System.out.println("Error updating status: " + e.getMessage());
    }
}
}
