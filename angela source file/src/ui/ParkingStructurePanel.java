package ui;

import dao.ParkingSpotDAO;
import model.ParkingSpot;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParkingStructurePanel extends JPanel {
    private ParkingSpotDAO dao;
    private JTabbedPane tabbedPane;

    public ParkingStructurePanel() {
        this.dao = new ParkingSpotDAO();
        this.setLayout(new BorderLayout());

        // 1. TOP CONTAINER: Holds both Legend AND the Note
        JPanel topContainer = new JPanel(new BorderLayout());

        // Add the Color Key (Top half of container)
        topContainer.add(createLegendPanel(), BorderLayout.NORTH);

        // Add the Important Note (Bottom half of container)
        topContainer.add(createNotePanel(), BorderLayout.SOUTH);

        // Add the whole container to the top of the window
        add(topContainer, BorderLayout.NORTH);

        // 2. CENTER: The Floors
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        loadSpots();
    }

    // --- PART A: THE COLOR KEY ---
    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(new Color(230, 230, 230)); // Gray background
        panel.setBorder(new EmptyBorder(5, 5, 0, 5)); // Remove bottom padding so note sits close

        panel.add(createLegendItem("Compact (RM 2/hr)", Color.PINK));
        panel.add(createLegendItem("Regular (RM 5/hr)", Color.GREEN));
        panel.add(createLegendItem("Handicapped (RM 2/hr)", Color.BLUE));
        panel.add(createLegendItem("Reserved (RM 10/hr)", Color.ORANGE));
        panel.add(createLegendItem("Occupied", Color.RED));

        return panel;
    }

    private JLabel createLegendItem(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(color);
        label.setForeground(color == Color.BLUE ? Color.WHITE : Color.BLACK);
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        label.setPreferredSize(new Dimension(150, 30));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        return label;
    }

    // --- PART B: THE IMPORTANT NOTE (Bold & Italic) ---
    private JPanel createNotePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(new Color(255, 255, 204)); // Light Yellow to grab attention
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY)); // Line at bottom

        JLabel noteLabel = new JLabel("NOTE: Handicapped spots are FREE only if a handicapped card holder vehicle parks in the spot.");

        // STYLE: Bold + Italic as requested
        noteLabel.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 12));
        noteLabel.setForeground(new Color(204, 0, 0)); // Dark Red text for urgency

        panel.add(noteLabel);
        return panel;
    }

    // --- LOAD DATA ---
    public void loadSpots() {
        tabbedPane.removeAll();
        List<ParkingSpot> allSpots = dao.getAllSpots();

        Map<Integer, List<ParkingSpot>> spotsByFloor = allSpots.stream()
                .collect(Collectors.groupingBy(ParkingSpot::getFloorNumber));

        for (Integer floorNum : spotsByFloor.keySet()) {
            JPanel floorPanel = createFloorPanel(spotsByFloor.get(floorNum));
            tabbedPane.addTab("Floor " + floorNum, floorPanel);
        }

        revalidate();
        repaint();
    }

    // --- DRAW GRID ---
    private JPanel createFloorPanel(List<ParkingSpot> spots) {
        JPanel panel = new JPanel(new GridLayout(3, 10, 5, 5));

        for (ParkingSpot spot : spots) {
            JButton btn = new JButton();

            String label = "<html><center><b>" + spot.getSpotId() + "</b><br>" +
                    "<small>RM " + spot.getHourlyRate() + "/hr</small></center></html>";
            btn.setText(label);
            btn.setPreferredSize(new Dimension(100, 60));

            String specialNote = spot.getType().equals("Handicapped") ? " (Free with Card)" : "";
            btn.setToolTipText("Type: " + spot.getType() + " | Rate: RM " + spot.getHourlyRate() + specialNote);

            if (spot.isOccupied()) {
                btn.setBackground(Color.RED);
                btn.setText("<html><center>OCCUPIED<br>" + spot.getSpotId() + "</center></html>");
            } else {
                switch (spot.getType()) {
                    case "Compact": btn.setBackground(Color.PINK); break;
                    case "Regular": btn.setBackground(Color.GREEN); break;
                    case "Handicapped": btn.setBackground(Color.BLUE); btn.setForeground(Color.WHITE); break;
                    case "Reserved": btn.setBackground(Color.ORANGE); break;
                    default: btn.setBackground(Color.LIGHT_GRAY);
                }
            }
            panel.add(btn);
        }
        return panel;
    }
}
