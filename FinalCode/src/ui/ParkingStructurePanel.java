package ui;

import facade.ParkingSystemFacade;
import model.ParkingSpot;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParkingStructurePanel extends JPanel {
    private ParkingSystemFacade facade;
    private JTabbedPane tabbedPane;

    public ParkingStructurePanel(ParkingSystemFacade facade) {
        this.facade = facade;
        this.setLayout(new BorderLayout());

        // Register for refresh notifications (this keeps auto-refresh working)
        facade.addRefreshListener(this::refreshDisplay);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(createLegendPanel(), BorderLayout.NORTH);
        topContainer.add(createNotePanel(), BorderLayout.CENTER);
        topContainer.add(createRefreshButtonPanel(), BorderLayout.SOUTH); // Added this
        add(topContainer, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        loadSpots();
    }

    public void refreshDisplay() {
        loadSpots();
    }

    private JPanel createRefreshButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(new Color(240, 240, 240));

        JButton refreshBtn = new JButton("🔄 Refresh Display");
        refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refreshDisplay());

        panel.add(refreshBtn);
        return panel;
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(new Color(230, 230, 230));
        panel.setBorder(new EmptyBorder(5, 5, 0, 5));

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

    private JPanel createNotePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(new Color(255, 255, 204));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        JLabel noteLabel = new JLabel("NOTE: Handicapped spots are FREE only if a handicapped card holder vehicle parks in the spot.");
        noteLabel.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 12));
        noteLabel.setForeground(new Color(204, 0, 0));

        panel.add(noteLabel);
        return panel;
    }

    public void loadSpots() {
        tabbedPane.removeAll();
        List<ParkingSpot> allSpots = facade.getAllSpots();

        Map<Integer, List<ParkingSpot>> spotsByFloor = allSpots.stream()
                .collect(Collectors.groupingBy(ParkingSpot::getFloorNumber));

        for (Integer floorNum : spotsByFloor.keySet()) {
            JPanel floorPanel = createFloorPanel(spotsByFloor.get(floorNum));
            tabbedPane.addTab("Floor " + floorNum, floorPanel);
        }

        revalidate();
        repaint();
    }

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
