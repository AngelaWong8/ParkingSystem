package ui;

import facade.ParkingSystemFacade;
import model.*;
import dao.*;
import database.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private JLabel profitLabel; // Store reference to profit label

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

        String[] columns = {"License Plate", "Spot", "Entry Time", "Duration", "Est. Fee"};
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
        vehiclesTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(vehiclesTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRevenuePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Summary cards at top
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Revenue Card
        JPanel revenueCard = new JPanel();
        revenueCard.setLayout(new BoxLayout(revenueCard, BoxLayout.Y_AXIS));
        revenueCard.setBackground(new Color(230, 255, 230));
        revenueCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        revenueCard.setPreferredSize(new Dimension(150, 100));

        JLabel revenueTitle = new JLabel("TOTAL REVENUE");
        revenueTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        revenueTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        totalRevenueLabel = new JLabel("RM 0.00");
        totalRevenueLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        totalRevenueLabel.setForeground(new Color(0, 150, 0));
        totalRevenueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        revenueCard.add(Box.createVerticalGlue());
        revenueCard.add(revenueTitle);
        revenueCard.add(Box.createRigidArea(new Dimension(0, 5)));
        revenueCard.add(totalRevenueLabel);
        revenueCard.add(Box.createVerticalGlue());

        // Fines Card
        JPanel finesCard = new JPanel();
        finesCard.setLayout(new BoxLayout(finesCard, BoxLayout.Y_AXIS));
        finesCard.setBackground(new Color(255, 230, 230));
        finesCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        finesCard.setPreferredSize(new Dimension(150, 100));

        JLabel finesTitle = new JLabel("TOTAL FINES");
        finesTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        finesTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        totalFinesLabel = new JLabel("RM 0.00");
        totalFinesLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        totalFinesLabel.setForeground(Color.RED.darker());
        totalFinesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        finesCard.add(Box.createVerticalGlue());
        finesCard.add(finesTitle);
        finesCard.add(Box.createRigidArea(new Dimension(0, 5)));
        finesCard.add(totalFinesLabel);
        finesCard.add(Box.createVerticalGlue());

        // Occupancy Card
        JPanel occupancyCard = new JPanel();
        occupancyCard.setLayout(new BoxLayout(occupancyCard, BoxLayout.Y_AXIS));
        occupancyCard.setBackground(new Color(230, 230, 255));
        occupancyCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        occupancyCard.setPreferredSize(new Dimension(150, 100));

        JLabel occupancyTitle = new JLabel("OCCUPANCY");
        occupancyTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        occupancyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        occupancyLabel = new JLabel("0/0 (0%)");
        occupancyLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        occupancyLabel.setForeground(Color.BLUE);
        occupancyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        occupancyCard.add(Box.createVerticalGlue());
        occupancyCard.add(occupancyTitle);
        occupancyCard.add(Box.createRigidArea(new Dimension(0, 5)));
        occupancyCard.add(occupancyLabel);
        occupancyCard.add(Box.createVerticalGlue());

        // Net Profit Card
        JPanel profitCard = new JPanel();
        profitCard.setLayout(new BoxLayout(profitCard, BoxLayout.Y_AXIS));
        profitCard.setBackground(new Color(255, 255, 200));
        profitCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        profitCard.setPreferredSize(new Dimension(150, 100));

        JLabel profitTitle = new JLabel("NET PROFIT");
        profitTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        profitTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        profitLabel = new JLabel("RM 0.00");
        profitLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        profitLabel.setForeground(new Color(0, 100, 0));
        profitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        profitCard.add(Box.createVerticalGlue());
        profitCard.add(profitTitle);
        profitCard.add(Box.createRigidArea(new Dimension(0, 5)));
        profitCard.add(profitLabel);
        profitCard.add(Box.createVerticalGlue());

        summaryPanel.add(revenueCard);
        summaryPanel.add(finesCard);
        summaryPanel.add(occupancyCard);
        summaryPanel.add(profitCard);

        // Revenue breakdown table
        String[] columns = {"Payment Type", "Amount", "Count"};
        revenueTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        revenueTable = new JTable(revenueTableModel);
        revenueTable.setRowHeight(30);

        JScrollPane scrollPane = new JScrollPane(revenueTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Today's Payment Breakdown"));

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
        String[] columns = {"License Plate", "Fine Type", "Amount", "Paid", "Outstanding", "Overstay (mins)", "Scheme", "Date"};
        finesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        finesTable = new JTable(finesTableModel);
        finesTable.setRowHeight(25);
        finesTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        finesTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        finesTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        finesTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        finesTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        finesTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        finesTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        finesTable.getColumnModel().getColumn(7).setPreferredWidth(100);

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
            e.printStackTrace();
        }
    }

    private void refreshVehiclesTab() throws SQLException {
        vehiclesTableModel.setRowCount(0);

        List<Ticket> tickets = new TicketDAO().getAllActiveTickets();
        for (Ticket ticket : tickets) {
            long mins = Duration.between(ticket.getEntryTime(), LocalDateTime.now()).toMinutes();
            long hours = (long) Math.ceil(mins / 60.0);
            if (hours == 0) hours = 1;
            String duration = hours + " hr" + (hours > 1 ? "s" : "");

            // Get spot rate
            ParkingSpot spot = new ParkingSpotDAO().getSpotBySpotId(ticket.getSpotId());
            double rate = (spot != null) ? spot.getHourlyRate() : 5.0;
            double estFee = hours * rate;

            vehiclesTableModel.addRow(new Object[]{
                    ticket.getLicensePlate(),
                    ticket.getSpotId(),
                    ticket.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    duration,
                    String.format("RM %.2f", estFee)
            });
        }

        if (tickets.isEmpty()) {
            vehiclesTableModel.addRow(new Object[]{"No vehicles currently parked", "", "", "", ""});
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
        profitLabel.setText(String.format("RM %.2f", revenue + fines));

        // Refresh revenue breakdown
        revenueTableModel.setRowCount(0);

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Get payment breakdown
        String sql = "SELECT payment_method, COUNT(*) as count, SUM(amount_paid) as total " +
                "FROM payments WHERE date(payment_time) = ? GROUP BY payment_method";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String method = rs.getString("payment_method");
                int count = rs.getInt("count");
                double total_amount = rs.getDouble("total");

                revenueTableModel.addRow(new Object[]{
                        method,
                        String.format("RM %.2f", total_amount),
                        count
                });
            }

            // If no payments today, show a message
            if (revenueTableModel.getRowCount() == 0) {
                revenueTableModel.addRow(new Object[]{"No payments today", "RM 0.00", 0});
            }
        }
    }

    private void refreshFinesTab() throws SQLException {
        finesTableModel.setRowCount(0);

        String sql = "SELECT license_plate, fine_type, fine_amount, paid_amount, " +
                "(fine_amount - paid_amount) as outstanding, overstay_minutes, " +
                "calculation_method, created_date " +
                "FROM fines WHERE is_paid = 0 ORDER BY created_date DESC";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            double totalOutstanding = 0;
            while (rs.next()) {
                double outstanding = rs.getDouble("outstanding");
                totalOutstanding += outstanding;

                finesTableModel.addRow(new Object[]{
                        rs.getString("license_plate"),
                        rs.getString("fine_type"),
                        String.format("RM %.2f", rs.getDouble("fine_amount")),
                        String.format("RM %.2f", rs.getDouble("paid_amount")),
                        String.format("RM %.2f", outstanding),
                        rs.getInt("overstay_minutes"),
                        rs.getString("calculation_method") != null ? rs.getString("calculation_method") : "Fixed",
                        rs.getString("created_date").substring(0, 10)
                });
            }

            if (finesTableModel.getRowCount() == 0) {
                finesTableModel.addRow(new Object[]{"No outstanding fines", "", "", "", "", "", "", ""});
            }

            totalOutstandingLabel.setText(String.format("Total Outstanding: RM %.2f", totalOutstanding));
        } catch (SQLException e) {
            finesTableModel.addRow(new Object[]{"Fine data unavailable", "", "", "", "", "", "", ""});
            totalOutstandingLabel.setText("Total Outstanding: RM 0.00");
            e.printStackTrace();
        }
    }
}
