package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:parking.db";
    private static Connection connection = null;
    
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        }
        return connection;
    }
    
    private static void initializeDatabase() {
        try (Statement stmt = getConnection().createStatement()) {
            // Create vehicles table
            stmt.execute("CREATE TABLE IF NOT EXISTS vehicles (" +
                        "license_plate TEXT PRIMARY KEY, " +
                        "vehicle_type TEXT NOT NULL, " +
                        "has_handicapped_card INTEGER DEFAULT 0)");
            
            // Create tickets table (active parkings)
            stmt.execute("CREATE TABLE IF NOT EXISTS tickets (" +
                        "ticket_id TEXT PRIMARY KEY, " +
                        "license_plate TEXT NOT NULL, " +
                        "spot_id TEXT NOT NULL, " +
                        "entry_time TEXT NOT NULL, " +
                        "FOREIGN KEY (license_plate) REFERENCES vehicles(license_plate))");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}