package ui;

import facade.ParkingSystemFacade;
import dao.ParkingSpotDAO;
import database.DatabaseConnection;
import javax.swing.*;

public class MainFrame {
    public static void main(String[] args) {
        // Setup database
        DatabaseConnection.createTables();

        // Initialize spots if empty
        ParkingSpotDAO dao = new ParkingSpotDAO();
        dao.initializeSpotsIfEmpty();

        // Create the one facade to rule them all
        ParkingSystemFacade facade = new ParkingSystemFacade();

        // Create main frame
        JFrame frame = new JFrame("Parking Management System - Integrated");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 800);
        frame.setLocationRelativeTo(null);

        // Create tabs - EVERY panel gets the SAME facade
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Parking Structure", new ParkingStructurePanel(facade));  
        tabs.addTab("Vehicle Entry", new EntryPanel(facade));
        tabs.addTab("Exit & Payment", new ExitPanel(facade));
        tabs.addTab("Admin", new AdminPanel(facade));
        tabs.addTab("Reports", new ReportPanel(facade));

        frame.add(tabs);
        frame.setVisible(true);
    }
}
