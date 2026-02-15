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

    // Angela - This is the most important method in this class
    // It creates the parking lot in the database ONLY if it doesn't exist yet
    public void initializeSpotsIfEmpty() {
        String checkSql = "SELECT COUNT(*) FROM parking_spots";
        String insertSql = "INSERT INTO parking_spots(id, spot_id_str, type, floor_number, row_number, hourly_rate) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            // Angela - First, I check: Is the parking lot empty?
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Generating spots for all floors (With Handicapped on EACH floor)...");

                PreparedStatement pstmt = conn.prepareStatement(insertSql);
                int globalId = 1;

                // Angela - I use a "Nested Loop" to build the structure:
                // Loop 1: Floors (1 to 3)
                for (int f = 1; f <= 3; f++) {
                    // Loop 2: Rows (1 to 3)
                    for (int r = 1; r <= 3; r++) {
                        // Loop 3: Spots (1 to 10)
                        for (int s = 1; s <= 10; s++) {

                            // Angela - I create a unique ID string like "F1-R1-S1" so humans can read it.
                            String clientSpotId = "F" + f + "-R" + r + "-S" + s;

                            // Default settings for a normal spot
                            String type = "Regular";
                            double rate = 5.0;

                            // --- ANGELA: UPDATED LAYOUT RULES ---

                            // 1. HANDICAPPED: Row 1, Spots 1 & 2 (EVERY FLOOR)
                            if (r == 1 && s <= 2) {
                                type = "Handicapped";
                                rate = 2.0;
                            }
                            // 2. COMPACT: Row 1, Spots 3, 4, 5 (EVERY FLOOR)
                            else if (r == 1 && s <= 5) {
                                type = "Compact";
                                rate = 2.0;
                            }
                            // 3. RESERVED: Row 3, Spots 9 & 10 (EVERY FLOOR)
                            // Angela - I removed "f == 3" so this applies to all floors now
                            else if (r == 3 && s >= 9) {
                                type = "Reserved";
                                rate = 10.0;
                            }
                            // ------------------------------------

                            // Angela - Now I save this specific spot into the "Batch" to be sent to the database.
                            pstmt.setInt(1, globalId++);
                            pstmt.setString(2, clientSpotId);
                            pstmt.setString(3, type);
                            pstmt.setInt(4, f);
                            pstmt.setInt(5, r);
                            pstmt.setDouble(6, rate);
                            pstmt.addBatch();
                        }
                    }
                }
                // Angela - Execute the batch
                pstmt.executeBatch();
                System.out.println("Database initialized: 2 Handicapped spots added to EVERY floor.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Angela - This method fetches the entire map. The "Parking Structure Panel" uses this to draw the red/green boxes.
    public List<ParkingSpot> getAllSpots() {
        List<ParkingSpot> spots = new ArrayList<>();
        String sql = "SELECT * FROM parking_spots ORDER BY id";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Angela - Converting raw Database rows into Java Objects
                ParkingSpot spot = new ParkingSpot(
                        rs.getInt("id"),
                        rs.getString("spot_id_str"),
                        rs.getString("type"),
                        rs.getInt("floor_number"),
                        rs.getInt("row_number"),
                        rs.getDouble("hourly_rate")
                );
                // Angela - Checking if the spot is currently red (occupied) or green (free)
                spot.setOccupied(rs.getInt("is_occupied") == 1);
                spots.add(spot);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return spots;
    }

    // Angela - This helper method finds a single spot object using its name (for example: "F1-R1-S1")
    // The Entry Panel needs this to link a Ticket to a Spot
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

    // Angela - This method is the "Switch". It turns a spot RED (Occupied) or GREEN (Free).
    // It is called by the Facade when a car enters or exits.
    public void updateSpotStatus(int id, boolean occupied) {
        String sql = "UPDATE parking_spots SET is_occupied = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Angela - Translating Java boolean (true/false) to SQL integer (1/0)
            pstmt.setInt(1, occupied ? 1 : 0);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();

            System.out.println("Spot " + id + " status updated in DB.");
        } catch (SQLException e) {
            System.out.println("Error updating status: " + e.getMessage());
        }
    }
}
