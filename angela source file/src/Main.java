import dao.ParkingSpotDAO;
import ui.ParkingStructurePanel;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // 1. SETUP DB (Keep this!)
        ParkingSpotDAO dao = new ParkingSpotDAO();
        dao.createTable();
        dao.initializeSpotsIfEmpty();

        // 2. LAUNCH GUI (New Part!)
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Parking Lot System - Group 3 T17L");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Add your panel to the frame
            frame.add(new ParkingStructurePanel());

            frame.pack(); // Adjust size to fit buttons
            frame.setLocationRelativeTo(null); // Center on screen
            frame.setVisible(true);
        });
    }
}
