package dao;

import model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {
    
    public void saveVehicle(Vehicle vehicle) throws SQLException {
        String sql = "INSERT OR REPLACE INTO vehicles (license_plate, vehicle_type, has_handicapped_card) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, vehicle.getLicensePlate());
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
        
        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, licensePlate);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString("vehicle_type");
                    boolean hasCard = rs.getInt("has_handicapped_card") == 1;
                    
                    switch (type) {
                        case "CAR": return new Car(licensePlate);
                        case "MOTORCYCLE": return new Motorcycle(licensePlate);
                        case "SUV": return new SUV(licensePlate);
                        case "HANDICAPPED": return new HandicappedVehicle(licensePlate, hasCard);
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
        
        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
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
