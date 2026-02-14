package ui;

import facade.ParkingSystemFacade;
import dao.ReportDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ReportPanel extends JPanel {

    private ParkingSystemFacade facade;
    private JTable revenueTable;
    private DefaultTableModel tableModel;
    private JLabel totalRevenueLabel;
    private JLabel totalFinesLabel;
    private JLabel occupancyLabel;

    public ReportPanel(ParkingSystemFacade facade) {
        this.facade = facade;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initUI();
        refreshData();
    }

    private void initUI() {
        JPanel summaryPanel = createSummaryPanel();
        add(summaryPanel, BorderLayout.NORTH);

        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel revenueCard = new JPanel(new GridBagLayout());
        revenueCard.setBackground(new Color(230, 255, 230));
        revenueCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JLabel revenueTitle = new JLabel("TOTAL REVENUE TODAY");
        revenueTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        totalRevenueLabel = new JLabel("RM 0.00");
        totalRevenueLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        totalRevenueLabel.setForeground(new Color(0, 150, 0));
        revenueCard.add(revenueTitle);
        revenueCard.add(totalRevenueLabel);

        JPanel finesCard = new JPanel(new GridBagLayout());
        finesCard.setBackground(new Color(255, 230, 230));
        finesCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JLabel finesTitle = new JLabel("TOTAL FINES TODAY");
        finesTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        totalFinesLabel = new JLabel("RM 0.00");
        totalFinesLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        totalFinesLabel.setForeground(Color.RED.darker());
        finesCard.add(finesTitle);
        finesCard.add(totalFinesLabel);

        JPanel occupancyCard = new JPanel(new GridBagLayout());
        occupancyCard.setBackground(new Color(230, 230, 255));
        occupancyCard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JLabel occupancyTitle = new JLabel("CURRENT OCCUPANCY");
        occupancyTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        occupancyLabel = new JLabel("0/0 (0%)");
        occupancyLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        occupancyLabel.setForeground(Color.BLUE);
        occupancyCard.add(occupancyTitle);
        occupancyCard.add(occupancyLabel);

        panel.add(revenueCard);
        panel.add(finesCard);
        panel.add(occupancyCard);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Hourly Occupancy Report"));

        String[] columns = {"Hour", "Vehicles Parked", "Utilization"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        revenueTable = new JTable(tableModel);
        revenueTable.setRowHeight(30);
        revenueTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        revenueTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        revenueTable.getColumnModel().getColumn(2).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(revenueTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton exportBtn = new JButton("Export to CSV");
        exportBtn.addActionListener(e -> exportToCsv());

        JButton printBtn = new JButton("Print Report");
        printBtn.addActionListener(e -> printReport());

        panel.add(exportBtn);
        panel.add(printBtn);

        return panel;
    }

    private void refreshData() {
        try {
            double revenue = facade.getTotalRevenueToday();
            double fines = facade.getTotalFinesToday();
            int occupied = facade.getCurrentOccupancy();
            int total = facade.getTotalSpots();
            double rate = facade.getOccupancyRate();

            totalRevenueLabel.setText(String.format("RM %.2f", revenue));
            totalFinesLabel.setText(String.format("RM %.2f", fines));
            occupancyLabel.setText(String.format("%d/%d (%.1f%%)", occupied, total, rate));

            tableModel.setRowCount(0);
            List<Object[]> hourlyData = new ReportDAO().getHourlyOccupancy();
            for (Object[] row : hourlyData) {
                String hour = row[0].toString();
                int count = (int) row[1];
                String utilization = count > 0 ? "Active" : "Low";
                tableModel.addRow(new Object[]{hour + ":00", count, utilization});
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void exportToCsv() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".csv")) path += ".csv";

            try (java.io.PrintWriter writer = new java.io.PrintWriter(path)) {
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.print(tableModel.getColumnName(i));
                    if (i < tableModel.getColumnCount() - 1) writer.print(",");
                }
                writer.println();

                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        writer.print(tableModel.getValueAt(row, col));
                        if (col < tableModel.getColumnCount() - 1) writer.print(",");
                    }
                    writer.println();
                }

                JOptionPane.showMessageDialog(this,
                        "Report exported to: " + path,
                        "Export Successful",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error exporting: " + e.getMessage(),
                        "Export Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void printReport() {
        try {
            revenueTable.print();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error printing: " + e.getMessage(),
                    "Print Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}