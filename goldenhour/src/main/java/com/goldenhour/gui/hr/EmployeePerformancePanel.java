package com.goldenhour.gui.hr;

import com.goldenhour.categories.Employee;
import com.goldenhour.categories.Sales;
import com.goldenhour.storage.DatabaseHandler;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * EmployeePerformancePanel - A dashboard for managers to view employee sales
 * performance.
 * 
 * Features:
 * - Table showing Employee Name, Total Sales Amount, and Transaction Count.
 * - Sortable by Name, Total Sales, and Number of Transactions.
 * - Auto-refreshes when the panel becomes visible.
 */
public class EmployeePerformancePanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JComboBox<String> timeFilterCombo;
    private JComboBox<String> nameSortCombo;
    private JComboBox<String> salesSortCombo;
    private JComboBox<String> transSortCombo;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public EmployeePerformancePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // 1. MAIN CARD
        JPanel cardPanel = new JPanel(new BorderLayout(0, 20));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(25, 25, 25, 25)));

        // 2. HEADER
        JPanel topPanel = new JPanel(new BorderLayout(0, 15));
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Employee Performance Analytics");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(52, 71, 103));
        topPanel.add(title, BorderLayout.NORTH);

        topPanel.add(createControlBar(), BorderLayout.CENTER);
        cardPanel.add(topPanel, BorderLayout.NORTH);

        // 3. TABLE
        cardPanel.add(createModernTable(), BorderLayout.CENTER);

        add(cardPanel, BorderLayout.CENTER);

        // Auto-refresh when panel becomes visible
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                resetToDefaults();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });

        refreshData();
    }

    private JScrollPane createModernTable() {
        String[] columns = { "Employee Name", "Total Sales (RM)", "Number of Transactions" };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1)
                    return Double.class;
                if (columnIndex == 2)
                    return Integer.class;
                return String.class;
            }
        };

        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Styling
        table.setRowHeight(45);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(230, 230, 230));
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(240, 248, 255));
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(Color.WHITE);
                setForeground(new Color(160, 174, 192));
                setFont(new Font("SansSerif", Font.BOLD, 12));
                setBorder(new MatteBorder(0, 0, 2, 0, new Color(230, 230, 230)));
                if (column == 1 || column == 2)
                    setHorizontalAlignment(JLabel.RIGHT);
                else
                    setHorizontalAlignment(JLabel.LEFT);
                return this;
            }
        });
        header.setPreferredSize(new Dimension(0, 45));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                if (value instanceof Double)
                    value = String.format("RM %.2f", (Double) value);
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected)
                    setBackground(Color.WHITE);
                if (col == 1 || col == 2) {
                    setHorizontalAlignment(JLabel.RIGHT);
                    setBorder(new EmptyBorder(0, 0, 0, 10));
                } else {
                    setHorizontalAlignment(JLabel.LEFT);
                    setBorder(new EmptyBorder(0, 10, 0, 0));
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(new Color(230, 230, 230)));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel createControlBar() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 15);
        gbc.anchor = GridBagConstraints.WEST;

        // --- ROW 1: TIME FILTER ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createLabel("Time Period:"), gbc);

        gbc.gridx = 1;
        timeFilterCombo = new JComboBox<>(new String[] { "All Time", "Today", "This Week", "This Month", "This Year" });
        styleCombo(timeFilterCombo);
        timeFilterCombo.setSelectedIndex(1);
        timeFilterCombo.addActionListener(e -> refreshData());
        panel.add(timeFilterCombo, gbc);

        gbc.gridx = 6; // Push refresh to the end of Row 1
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        JButton refreshBtn = new JButton("Refresh Data");
        styleSecondaryButton(refreshBtn);
        refreshBtn.addActionListener(e -> resetToDefaults());
        panel.add(refreshBtn, gbc);

        // --- ROW 2: SORTING ---
        gbc.weightx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;

        // Name Sort
        gbc.gridx = 0;
        panel.add(createLabel("Sort Name:"), gbc);
        gbc.gridx = 1;
        nameSortCombo = new JComboBox<>(new String[] { "-", "A to Z", "Z to A" });
        styleCombo(nameSortCombo);
        nameSortCombo.addActionListener(e -> {
            if (nameSortCombo.getSelectedIndex() > 0) {
                salesSortCombo.setSelectedIndex(0);
                transSortCombo.setSelectedIndex(0);
                applySorting(0, nameSortCombo.getSelectedIndex() == 1);
            }
        });
        panel.add(nameSortCombo, gbc);

        // Sales Sort
        gbc.gridx = 2;
        panel.add(createLabel("Sort Sales:"), gbc);
        gbc.gridx = 3;
        salesSortCombo = new JComboBox<>(new String[] { "-", "Descending", "Ascending" });
        styleCombo(salesSortCombo);
        salesSortCombo.addActionListener(e -> {
            if (salesSortCombo.getSelectedIndex() > 0) {
                nameSortCombo.setSelectedIndex(0);
                transSortCombo.setSelectedIndex(0);
                applySorting(1, salesSortCombo.getSelectedIndex() == 2);
            }
        });
        panel.add(salesSortCombo, gbc);

        // Trans Sort
        gbc.gridx = 4;
        panel.add(createLabel("Sort Trans:"), gbc);
        gbc.gridx = 5;
        transSortCombo = new JComboBox<>(new String[] { "-", "Descending", "Ascending" });
        styleCombo(transSortCombo);
        transSortCombo.addActionListener(e -> {
            if (transSortCombo.getSelectedIndex() > 0) {
                nameSortCombo.setSelectedIndex(0);
                salesSortCombo.setSelectedIndex(0);
                applySorting(2, transSortCombo.getSelectedIndex() == 2);
            }
        });
        panel.add(transSortCombo, gbc);

        return panel;
    }

    private void refreshData() {
        if (tableModel == null)
            return;
        tableModel.setRowCount(0);

        List<Employee> employees = DatabaseHandler.fetchAllEmployees();
        List<Sales> allSales = DatabaseHandler.fetchAllSales();

        // 1. Calculate Date Range for Filter
        LocalDate now = LocalDate.now();
        LocalDate startDate = null;
        String filterType = (timeFilterCombo != null) ? (String) timeFilterCombo.getSelectedItem() : "All Time";

        if ("Today".equals(filterType)) {
            startDate = now;
        } else if ("This Week".equals(filterType)) {
            startDate = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if ("This Month".equals(filterType)) {
            startDate = now.with(TemporalAdjusters.firstDayOfMonth());
        } else if ("This Year".equals(filterType)) {
            startDate = now.with(TemporalAdjusters.firstDayOfYear());
        }

        // Map to store performance: Employee Name -> Performance Data
        Map<String, PerformanceData> performanceMap = new HashMap<>();

        // Initialize all employees with 0 performance
        for (Employee emp : employees) {
            // NEGLECT System Seeder
            if ("System Seeder".equalsIgnoreCase(emp.getName()))
                continue;

            performanceMap.put(emp.getName(), new PerformanceData(emp.getName()));
        }

        // Aggregate sales data
        for (Sales sale : allSales) {
            String empName = sale.getEmployee();
            if (empName == null || empName.isEmpty() || "System Seeder".equalsIgnoreCase(empName))
                continue;

            // Apply Date Filter
            if (startDate != null) {
                try {
                    LocalDate saleDate = LocalDate.parse(sale.getDate(), dateFormatter);
                    if (saleDate.isBefore(startDate))
                        continue;
                } catch (Exception e) {
                    continue; // Skip invalid dates
                }
            }

            PerformanceData data = performanceMap.get(empName);
            if (data == null) {
                // In case a former employee made a sale but is no longer in employees list
                data = new PerformanceData(empName);
                performanceMap.put(empName, data);
            }
            data.totalSales += sale.getSubtotal();
            data.transactionCount++;
        }

        // Add to table
        for (PerformanceData data : performanceMap.values()) {
            tableModel.addRow(new Object[] {
                    data.employeeName,
                    data.totalSales,
                    data.transactionCount
            });
        }

    }

    private void resetToDefaults() {
        if (timeFilterCombo != null)
            timeFilterCombo.setSelectedIndex(1); // Reset to Today
        if (nameSortCombo != null)
            nameSortCombo.setSelectedIndex(0);
        if (salesSortCombo != null)
            salesSortCombo.setSelectedIndex(1); // Default Sort: Descending
        if (transSortCombo != null)
            transSortCombo.setSelectedIndex(0);

        applySorting(1, false); // Column 1: Sales, Descending
        refreshData();
    }

    private void applySorting(int column, boolean ascending) {
        List<RowSorter.SortKey> keys = new ArrayList<>();
        keys.add(new RowSorter.SortKey(column, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING));
        sorter.setSortKeys(keys);
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(100, 116, 139));
        return l;
    }

    private void styleCombo(JComboBox<String> box) {
        box.setBackground(Color.WHITE);
        box.setFont(new Font("SansSerif", Font.PLAIN, 13));
        box.setPreferredSize(new Dimension(110, 35));
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(52, 71, 103));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(Color.BLACK, 1, true));
        btn.setPreferredSize(new Dimension(120, 35));
    }

    // Helper class for data aggregation
    private static class PerformanceData {
        String employeeName;
        double totalSales = 0.0;
        int transactionCount = 0;

        PerformanceData(String name) {
            this.employeeName = name;
        }
    }
}
