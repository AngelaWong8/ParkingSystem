package ui;

import facade.ParkingSystemFacade;
import model.*;
import dao.*;
import database.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.List;

public class ReportPanel extends JPanel {

    private ParkingSystemFacade facade;
    private JTabbedPane reportTabs;

    // Tab 1: Current Vehicles
    private JTable vehiclesTable;
    private DefaultTableModel vehiclesTableModel;

    // Tab 2: Revenue & Occupancy
    private JTable revenueTable;
    private DefaultTableModel revenueTableModel;
    private JLabel totalRevenueLabel;
    private JLabel totalFinesLabel;
    private JLabel occupancyLabel;

    // Tab 3: Outstanding Fines
    private JTable finesTable;
    private DefaultTableModel finesTableModel;
    private JLabel totalOutstandingLabel;

    public ReportPanel(ParkingSystemFacade facade) {
        this.facade = facade;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initUI();
        facade.addRefreshListener(this::refreshData);
        refreshData();
    }

    private void initUI() {
        reportTabs = new JTabbedPane();

        // Tab 1: Current Vehicles
        reportTabs.addTab("Current Vehicles", createVehiclesPanel());

        // Tab 2: Revenue & Occupancy
        reportTabs.addTab("Revenue & Occupancy", createRevenuePanel());

        // Tab 3: Outstanding Fines
        reportTabs.addTab("Outstanding Fines", createFinesPanel());

        add(reportTabs, BorderLayout.CENTER);

        // Refresh button at bottom
        JButton refreshBtn = new JButton("Refresh All Reports");
        refreshBtn.addActionListener(e -> refreshData());
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(refreshBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createVehiclesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vehicles Currently in Parking Lot"));

        String[] columns = {"License Plate", "Spot", "Entry Time", "Duration"};
        vehiclesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        vehiclesTable = new JTable(vehiclesTableModel);
        vehiclesTable.setRowHeight(25);
        vehiclesTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        vehiclesTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        vehiclesTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        vehiclesTable.getColumnModel().getColumn(3).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(vehiclesTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRevenuePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Summary cards at top
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Revenue Card
        JPanel revenueCard = new JPanel();
        revenueCard.setLayout(new BoxLayout(revenueCard, BoxLayout.Y_AXIS));
        revenueCard.setBackground(new Color(230, 255, 230));
        revenueCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        revenueCard.setPreferredSize(new Dimension(200, 100));

        JLabel revenueTitle = new JLabel("TOTAL REVENUE TODAY");
        revenueTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        revenueTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        totalRevenueLabel = new JLabel("RM 0.00");
        totalRevenueLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        totalRevenueLabel.setForeground(new Color(0, 150, 0));
        totalRevenueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        revenueCard.add(Box.createVerticalGlue());
        revenueCard.add(revenueTitle);
        revenueCard.add(Box.createRigidArea(new Dimension(0, 10)));
        revenueCard.add(totalRevenueLabel);
        revenueCard.add(Box.createVerticalGlue());

        // Fines Card
        JPanel finesCard = new JPanel();
        finesCard.setLayout(new BoxLayout(finesCard, BoxLayout.Y_AXIS));
        finesCard.setBackground(new Color(255, 230, 230));
        finesCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        finesCard.setPreferredSize(new Dimension(200, 100));

        JLabel finesTitle = new JLabel("TOTAL FINES TODAY");
        finesTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        finesTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        totalFinesLabel = new JLabel("RM 0.00");
        totalFinesLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        totalFinesLabel.setForeground(Color.RED.darker());
        totalFinesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        finesCard.add(Box.createVerticalGlue());
        finesCard.add(finesTitle);
        finesCard.add(Box.createRigidArea(new Dimension(0, 10)));
        finesCard.add(totalFinesLabel);
        finesCard.add(Box.createVerticalGlue());

        // Occupancy Card
        JPanel occupancyCard = new JPanel();
        occupancyCard.setLayout(new BoxLayout(occupancyCard, BoxLayout.Y_AXIS));
        occupancyCard.setBackground(new Color(230, 230, 255));
        occupancyCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        occupancyCard.setPreferredSize(new Dimension(200, 100));

        JLabel occupancyTitle = new JLabel("CURRENT OCCUPANCY");
        occupancyTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        occupancyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        occupancyLabel = new JLabel("0/0 (0%)");
        occupancyLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        occupancyLabel.setForeground(Color.BLUE);
        occupancyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        occupancyCard.add(Box.createVerticalGlue());
        occupancyCard.add(occupancyTitle);
        occupancyCard.add(Box.createRigidArea(new Dimension(0, 10)));
        occupancyCard.add(occupancyLabel);
        occupancyCard.add(Box.createVerticalGlue());

        summaryPanel.add(revenueCard);
        summaryPanel.add(finesCard);
        summaryPanel.add(occupancyCard);

        // Hourly occupancy table
        String[] columns = {"Hour", "Vehicles Parked", "Utilization"};
        revenueTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        revenueTable = new JTable(revenueTableModel);
        revenueTable.setRowHeight(30);

        JScrollPane scrollPane = new JScrollPane(revenueTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Hourly Occupancy"));

        panel.add(summaryPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFinesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Total outstanding label
        totalOutstandingLabel = new JLabel("Total Outstanding: RM 0.00");
        totalOutstandingLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalOutstandingLabel.setForeground(Color.RED);
        totalOutstandingLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(totalOutstandingLabel);

        // Fines table
        String[] columns = {"License Plate", "Fine Amount", "Overstay (mins)", "Scheme", "Status"};
        finesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        finesTable = new JTable(finesTableModel);
        finesTable.setRowHeight(25);
        finesTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        finesTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        finesTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        finesTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        finesTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(finesTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Outstanding Fines List"));

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void refreshData() {
        try {
            refreshVehiclesTab();
            refreshRevenueTab();
            refreshFinesTab();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error refreshing reports: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshVehiclesTab() throws SQLException {
        vehiclesTableModel.setRowCount(0);

        List<Ticket> tickets = new TicketDAO().getAllActiveTickets();
        for (Ticket ticket : tickets) {
            long mins = java.time.Duration.between(ticket.getEntryTime(), java.time.LocalDateTime.now()).toMinutes();
            long hours = (long) Math.ceil(mins / 60.0);
            String duration = hours + " hr" + (hours > 1 ? "s" : "");

            vehiclesTableModel.addRow(new Object[]{
                    ticket.getLicensePlate(),
                    ticket.getSpotId(),
                    ticket.getEntryTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    duration
            });
        }

        if (tickets.isEmpty()) {
            vehiclesTableModel.addRow(new Object[]{"No vehicles currently parked", "", "", ""});
        }
    }

    private void refreshRevenueTab() throws SQLException {
        double revenue = facade.getTotalRevenueToday();
        double fines = facade.getTotalFinesToday();
        int occupied = facade.getCurrentOccupancy();
        int total = facade.getTotalSpots();
        double rate = facade.getOccupancyRate();

        totalRevenueLabel.setText(String.format("RM %.2f", revenue));
        totalFinesLabel.setText(String.format("RM %.2f", fines));
        occupancyLabel.setText(String.format("%d/%d (%.1f%%)", occupied, total, rate));

        revenueTableModel.setRowCount(0);
        List<Object[]> hourlyData = new ReportDAO().getHourlyOccupancy();
        for (Object[] row : hourlyData) {
            String hour = row[0].toString();
            int count = (int) row[1];
            String utilization = count > 0 ? "Active" : "Low";
            revenueTableModel.addRow(new Object[]{hour + ":00", count, utilization});
        }
    }

    private void refreshFinesTab() throws SQLException {
        finesTableModel.setRowCount(0);

        String sql = "SELECT license_plate, fine_amount, overstay_minutes, calculation_method, is_paid " +
                "FROM fines WHERE is_paid = 0";
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            double totalOutstanding = 0;
            while (rs.next()) {
                double amount = rs.getDouble("fine_amount");
                totalOutstanding += amount;

                finesTableModel.addRow(new Object[]{
                        rs.getString("license_plate"),
                        String.format("RM %.2f", amount),
                        rs.getInt("overstay_minutes"),
                        rs.getString("calculation_method"),
                        "Unpaid"
                });
            }

            if (finesTableModel.getRowCount() == 0) {
                finesTableModel.addRow(new Object[]{"No outstanding fines", "", "", "", ""});
            }

            totalOutstandingLabel.setText(String.format("Total Outstanding: RM %.2f", totalOutstanding));
        } catch (SQLException e) {
            finesTableModel.addRow(new Object[]{"Fine data unavailable", "", "", "", ""});
            totalOutstandingLabel.setText("Total Outstanding: RM 0.00");
        }
    }
}