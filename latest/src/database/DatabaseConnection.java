package database;

import java.sql.*;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:parking_system.db";
    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
        }
        return connection;
    }

    public static Connection connect() {
        try {
            return getConnection();
        } catch (SQLException e) {
            System.out.println("Connection Failed: " + e.getMessage());
            return null;
        }
    }

    public static void createTables() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Person A: parking_spots
            stmt.execute("CREATE TABLE IF NOT EXISTS parking_spots (" +
                    "id INTEGER PRIMARY KEY, " +
                    "spot_id_str TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "floor_number INTEGER, " +
                    "row_number INTEGER, " +
                    "hourly_rate REAL, " +
                    "is_occupied INTEGER DEFAULT 0)");

            // Person B: vehicles & tickets
            stmt.execute("CREATE TABLE IF NOT EXISTS vehicles (" +
                    "license_plate TEXT PRIMARY KEY, " +
                    "vehicle_type TEXT NOT NULL, " +
                    "has_handicapped_card INTEGER DEFAULT 0)");

            stmt.execute("CREATE TABLE IF NOT EXISTS tickets (" +
                    "ticket_id TEXT PRIMARY KEY, " +
                    "license_plate TEXT NOT NULL, " +
                    "spot_id TEXT NOT NULL, " +
                    "entry_time TEXT NOT NULL, " +
                    "is_fully_paid INTEGER DEFAULT 0, " +  // NEW COLUMN
                    "FOREIGN KEY (license_plate) REFERENCES vehicles(license_plate))");

            // Person C: payments & fines
            stmt.execute("CREATE TABLE IF NOT EXISTS payments (" +
                    "payment_id TEXT PRIMARY KEY, " +
                    "ticket_id TEXT NOT NULL, " +
                    "license_plate TEXT NOT NULL, " +
                    "amount_paid REAL NOT NULL, " +
                    "payment_method TEXT NOT NULL, " +
                    "payment_time TEXT NOT NULL, " +
                    "FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS fines (" +
                    "fine_id TEXT PRIMARY KEY, " +
                    "ticket_id TEXT NOT NULL, " +
                    "license_plate TEXT NOT NULL, " +
                    "fine_amount REAL NOT NULL, " +
                    "original_amount REAL NOT NULL, " +  // NEW: original parking fee
                    "paid_amount REAL DEFAULT 0, " +      // NEW: what they've paid
                    "overstay_minutes INTEGER NOT NULL, " +
                    "calculation_method TEXT NOT NULL, " +
                    "is_paid INTEGER DEFAULT 0, " +
                    "created_date TEXT NOT NULL, " +      // NEW: when fine was created
                    "FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id))");

            // Person D: daily_revenue
            stmt.execute("CREATE TABLE IF NOT EXISTS daily_revenue (" +
                    "date TEXT PRIMARY KEY, " +
                    "total_payments REAL DEFAULT 0, " +
                    "total_fines REAL DEFAULT 0, " +
                    "total_vehicles INTEGER DEFAULT 0)");

            stmt.execute("CREATE TABLE IF NOT EXISTS settings (" +
                    "key TEXT PRIMARY KEY, " +
                    "value TEXT NOT NULL)");

            System.out.println("Database tables created/verified.");

        } catch (SQLException e) {
            System.out.println("Error creating tables: " + e.getMessage());
        }
    }
}
