package ui;

import facade.ParkingSystemFacade;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.SQLException;

public class AdminPanel extends JPanel {

    private ParkingSystemFacade facade;
    private JLabel totalSpotsLabel;
    private JLabel occupiedLabel;
    private JLabel availableLabel;
    private JLabel occupancyRateLabel;
    private JLabel todayRevenueLabel;
    private JLabel todayFinesLabel;
    private JTextArea systemLogArea;
    private Timer refreshTimer;

    public AdminPanel(ParkingSystemFacade facade) {
        this.facade = facade;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initUI();
        startAutoRefresh();
        refreshData();
    }

    private void initUI() {
        JPanel statsPanel = createStatsPanel();
        add(statsPanel, BorderLayout.NORTH);

        JPanel logPanel = createLogPanel();
        add(logPanel, BorderLayout.CENTER);

        JPanel actionPanel = createActionPanel();
        add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 15, 15));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "System Statistics",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14)
        ));

        panel.add(new JLabel("Total Spots:", SwingConstants.RIGHT));
        totalSpotsLabel = new JLabel("0", SwingConstants.LEFT);
        totalSpotsLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        panel.add(totalSpotsLabel);

        panel.add(new JLabel("Occupied:", SwingConstants.RIGHT));
        occupiedLabel = new JLabel("0", SwingConstants.LEFT);
        occupiedLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        occupiedLabel.setForeground(Color.RED);
        panel.add(occupiedLabel);

        panel.add(new JLabel("Available:", SwingConstants.RIGHT));
        availableLabel = new JLabel("0", SwingConstants.LEFT);
        availableLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        availableLabel.setForeground(Color.GREEN.darker());
        panel.add(availableLabel);

        panel.add(new JLabel("Occupancy Rate:", SwingConstants.RIGHT));
        occupancyRateLabel = new JLabel("0%", SwingConstants.LEFT);
        occupancyRateLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        panel.add(occupancyRateLabel);

        panel.add(new JLabel("Today's Revenue:", SwingConstants.RIGHT));
        todayRevenueLabel = new JLabel("RM 0.00", SwingConstants.LEFT);
        todayRevenueLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        todayRevenueLabel.setForeground(new Color(0, 150, 0));
        panel.add(todayRevenueLabel);

        panel.add(new JLabel("Today's Fines:", SwingConstants.RIGHT));
        todayFinesLabel = new JLabel("RM 0.00", SwingConstants.LEFT);
        todayFinesLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        todayFinesLabel.setForeground(Color.RED.darker());
        panel.add(todayFinesLabel);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("System Activity Log"));

        systemLogArea = new JTextArea();
        systemLogArea.setEditable(false);
        systemLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        systemLogArea.setBackground(Color.BLACK);
        systemLogArea.setForeground(Color.GREEN);

        JScrollPane scrollPane = new JScrollPane(systemLogArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton refreshBtn = new JButton("Refresh Now");
        refreshBtn.addActionListener(e -> refreshData());

        JButton resetDbBtn = new JButton("Reset Database");
        resetDbBtn.addActionListener(e -> resetDatabase());

        JButton generateReportBtn = new JButton("Generate Full Report");
        generateReportBtn.addActionListener(e -> generateReport());

        panel.add(refreshBtn);
        panel.add(resetDbBtn);
        panel.add(generateReportBtn);

        return panel;
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(5000, e -> refreshData());
        refreshTimer.start();
    }

    private void refreshData() {
        try {
            int total = facade.getTotalSpots();
            int occupied = facade.getCurrentOccupancy();
            int available = total - occupied;
            double rate = facade.getOccupancyRate();
            double revenue = facade.getTotalRevenueToday();
            double fines = facade.getTotalFinesToday();

            totalSpotsLabel.setText(String.valueOf(total));
            occupiedLabel.setText(String.valueOf(occupied));
            availableLabel.setText(String.valueOf(available));
            occupancyRateLabel.setText(String.format("%.1f%%", rate));
            todayRevenueLabel.setText(String.format("RM %.2f", revenue));
            todayFinesLabel.setText(String.format("RM %.2f", fines));

            String timestamp = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            systemLogArea.append(String.format("[%s] Stats updated - Occupied: %d/%d (%.1f%%)%n",
                    timestamp, occupied, total, rate));

            systemLogArea.setCaretPosition(systemLogArea.getDocument().getLength());

        } catch (SQLException e) {
            systemLogArea.append("ERROR refreshing data: " + e.getMessage() + "\n");
        }
    }

    private void resetDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset ALL parking spots?",
                "Reset Database",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dao.ParkingSpotDAO dao = new dao.ParkingSpotDAO();
                dao.initializeSpotsIfEmpty();
                systemLogArea.append("*** DATABASE RESET - All spots reinitialized ***\n");
                refreshData();
            } catch (Exception e) {
                systemLogArea.append("ERROR resetting database: " + e.getMessage() + "\n");
            }
        }
    }

    private void generateReport() {
        JFrame reportFrame = new JFrame("Full System Report");
        reportFrame.setSize(800, 600);
        reportFrame.setLocationRelativeTo(this);
        reportFrame.add(new ReportPanel(facade));
        reportFrame.setVisible(true);
    }
}
