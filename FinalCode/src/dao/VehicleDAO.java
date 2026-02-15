package dao;

import model.*;
import database.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

    public void saveVehicle(Vehicle vehicle) throws SQLException {
        String sql = "INSERT OR REPLACE INTO vehicles (license_plate, vehicle_type, has_handicapped_card) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vehicle.getLicensePlate().toUpperCase());
            pstmt.setString(2, vehicle.getVehicleType());

            if (vehicle instanceof HandicappedVehicle) {
                pstmt.setInt(3, ((HandicappedVehicle) vehicle).hasHandicappedCard() ? 1 : 0);
            } else {
                pstmt.setInt(3, 0);
            }

            pstmt.executeUpdate();
        }
    }

    public Vehicle findVehicle(String licensePlate) throws SQLException {
        String sql = "SELECT * FROM vehicles WHERE license_plate = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, licensePlate.toUpperCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString("vehicle_type");
                    boolean hasCard = rs.getInt("has_handicapped_card") == 1;
                    String plate = rs.getString("license_plate");

                    switch (type) {
                        case "CAR": return new Car(plate);
                        case "MOTORCYCLE": return new Motorcycle(plate);
                        case "SUV": return new SUV(plate);
                        case "HANDICAPPED": return new HandicappedVehicle(plate, hasCard);
                        default: return null;
                    }
                }
            }
        }
        return null;
    }

    public List<Vehicle> getAllVehicles() throws SQLException {
        List<Vehicle> vehicles = new ArrayList<>();
        String sql = "SELECT * FROM vehicles";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String plate = rs.getString("license_plate");
                String type = rs.getString("vehicle_type");
                boolean hasCard = rs.getInt("has_handicapped_card") == 1;

                switch (type) {
                    case "CAR": vehicles.add(new Car(plate)); break;
                    case "MOTORCYCLE": vehicles.add(new Motorcycle(plate)); break;
                    case "SUV": vehicles.add(new SUV(plate)); break;
                    case "HANDICAPPED": vehicles.add(new HandicappedVehicle(plate, hasCard)); break;
                }
            }
        }
        return vehicles;
    }
}