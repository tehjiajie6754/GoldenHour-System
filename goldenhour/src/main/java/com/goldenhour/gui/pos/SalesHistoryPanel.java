package com.goldenhour.gui.pos;

import com.goldenhour.categories.Sales;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.storage.DatabaseHandler;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class SalesHistoryPanel extends JPanel {

    // --- Components ---
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // Controls
    private JTextField searchField;
    private JComboBox<String> filterMethodCombo;
    private JComboBox<String> sortCombo;

    // CUSTOM DATE PICKERS
    private ModernDatePicker fromDatePicker;
    private ModernDatePicker toDatePicker;

    private JLabel totalSalesLabel;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SalesHistoryPanel() {
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

        JLabel title = new JLabel("Sales History & Analytics");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(52, 71, 103));
        topPanel.add(title, BorderLayout.NORTH);

        topPanel.add(createControlBar(), BorderLayout.CENTER);
        cardPanel.add(topPanel, BorderLayout.NORTH);

        // 3. TABLE
        cardPanel.add(createModernTable(), BorderLayout.CENTER);

        // 4. FOOTER
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        totalSalesLabel = new JLabel("Total Sales: RM 0.00");
        totalSalesLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        totalSalesLabel.setForeground(new Color(40, 199, 111));
        footer.add(totalSalesLabel, BorderLayout.WEST);

        cardPanel.add(footer, BorderLayout.SOUTH);
        add(cardPanel, BorderLayout.CENTER);

        // Auto-refresh when panel becomes visible
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                loadSalesData();
                applySorting(); // Set default sort to Newest Date
                updateTotalSales();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });

        loadSalesData();
        updateTotalSales();
    }

    // =========================================================================
    // UI SETUP
    // =========================================================================

    private JScrollPane createModernTable() {
        String[] columns = { "Date", "Time", "Customer", "Model", "Qty", "Total (RM)", "Method" };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4)
                    return Integer.class;
                if (columnIndex == 5)
                    return Double.class;
                return String.class;
            }
        };

        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Custom comparator for Date (Column 0)
        sorter.setComparator(0, Comparator.comparing(String::toString));

        // Custom comparator for Time (Column 1) - Correct AM/PM sorting
        // (case-insensitive)
        DateTimeFormatter timeFormat = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("h:mm a")
                .toFormatter(Locale.ENGLISH);

        sorter.setComparator(1, (s1, s2) -> {
            try {
                // Remove any leading/trailing spaces and handle mid-string semicolons if they
                // exist
                String t1Str = ((String) s1).trim().replace(";", ":");
                String t2Str = ((String) s2).trim().replace(";", ":");

                LocalTime t1 = LocalTime.parse(t1Str, timeFormat);
                LocalTime t2 = LocalTime.parse(t2Str, timeFormat);
                return t1.compareTo(t2);
            } catch (Exception e) {
                // Fallback to simpler string comparison if parsing fails
                return ((String) s1).compareTo((String) s2);
            }
        });

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
                if (column == 4 || column == 5)
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
                    value = String.format("%.2f", (Double) value);
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected)
                    setBackground(Color.WHITE);
                if (col == 4 || col == 5) {
                    setHorizontalAlignment(JLabel.RIGHT);
                    setBorder(new EmptyBorder(0, 0, 0, 10));
                } else {
                    setHorizontalAlignment(JLabel.LEFT);
                    setBorder(new EmptyBorder(0, 10, 0, 0));
                }
                return this;
            }
        });

        table.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                if (value instanceof Double)
                    value = String.format("%.2f", (Double) value);
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected)
                    setBackground(Color.WHITE);
                setHorizontalAlignment(JLabel.RIGHT);
                setBorder(new EmptyBorder(0, 0, 0, 10));
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
        gbc.insets = new Insets(0, 0, 12, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createLabel("Search:"), gbc);

        gbc.gridx = 1;
        searchField = new JTextField(12);
        styleTextField(searchField);
        addLiveListener(searchField);
        panel.add(searchField, gbc);

        gbc.gridx = 2;
        panel.add(createLabel("Method:"), gbc);

        gbc.gridx = 3;
        String[] methods = { "All", "Cash", "Card", "E-Wallet", "Credit Card" };
        filterMethodCombo = new JComboBox<>(methods);
        styleCombo(filterMethodCombo);
        filterMethodCombo.addActionListener(e -> applyFilters());
        panel.add(filterMethodCombo, gbc);

        gbc.gridx = 4;
        panel.add(createLabel("Sort By:"), gbc);

        gbc.gridx = 5;
        String[] sortOptions = { "Newest Date", "Oldest Date", "Highest Amount", "Lowest Amount", "Customer (A-Z)",
                "Customer (Z-A)" };
        sortCombo = new JComboBox<>(sortOptions);
        styleCombo(sortCombo);
        sortCombo.addActionListener(e -> applySorting());
        panel.add(sortCombo, gbc);

        // Row 2 - CUSTOM DATE PICKERS
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(createLabel("From Date:"), gbc);

        gbc.gridx = 1;
        fromDatePicker = new ModernDatePicker();
        fromDatePicker.addDateChangeListener(e -> applyFilters());
        panel.add(fromDatePicker, gbc);

        gbc.gridx = 2;
        panel.add(createLabel("To Date:"), gbc);

        gbc.gridx = 3;
        toDatePicker = new ModernDatePicker();
        toDatePicker.addDateChangeListener(e -> applyFilters());
        panel.add(toDatePicker, gbc);

        gbc.gridx = 5;
        JButton resetBtn = new JButton("Reset All");
        styleSecondaryButton(resetBtn);
        resetBtn.addActionListener(e -> resetFilters());
        panel.add(resetBtn, gbc);

        return panel;
    }

    // =========================================================================
    // LOGIC
    // =========================================================================

    private void loadSalesData() {
        tableModel.setRowCount(0);
        DataLoad.allSales = DatabaseHandler.fetchAllSales();
        for (Sales s : DataLoad.allSales) {
            tableModel.addRow(new Object[] { s.getDate(), s.getTime(), s.getCustomerName(), s.getModel(),
                    s.getQuantity(), s.getSubtotal(), s.getTransactionMethod() });
        }
    }

    private void applyFilters() {
        List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();

        String text = searchField.getText().trim();
        if (!text.isEmpty())
            filters.add(RowFilter.regexFilter("(?i)" + text));

        String method = (String) filterMethodCombo.getSelectedItem();
        if (method != null && !"All".equals(method))
            filters.add(RowFilter.regexFilter("(?i)" + method, 6));

        // Use Date from Custom Picker
        String fromStr = fromDatePicker.getSelectedDate();
        String toStr = toDatePicker.getSelectedDate();

        if (fromStr != null || toStr != null) {
            filters.add(new RowFilter<DefaultTableModel, Object>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                    try {
                        String rowDateStr = (String) entry.getValue(0);
                        LocalDate rowDate = LocalDate.parse(rowDateStr, dateFormatter);
                        boolean afterFrom = (fromStr == null)
                                || !rowDate.isBefore(LocalDate.parse(fromStr, dateFormatter));
                        boolean beforeTo = (toStr == null) || !rowDate.isAfter(LocalDate.parse(toStr, dateFormatter));
                        return afterFrom && beforeTo;
                    } catch (DateTimeParseException e) {
                        return true;
                    }
                }
            });
        }

        if (filters.isEmpty())
            sorter.setRowFilter(null);
        else
            sorter.setRowFilter(RowFilter.andFilter(filters));

        updateTotalSales();
    }

    private void applySorting() {
        String selected = (String) sortCombo.getSelectedItem();
        List<RowSorter.SortKey> keys = new ArrayList<>();
        switch (selected) {
            case "Newest Date":
                keys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
                keys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
                break;
            case "Oldest Date":
                keys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
                keys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
                break;
            case "Highest Amount":
                keys.add(new RowSorter.SortKey(5, SortOrder.DESCENDING));
                break;
            case "Lowest Amount":
                keys.add(new RowSorter.SortKey(5, SortOrder.ASCENDING));
                break;
            case "Customer (A-Z)":
                keys.add(new RowSorter.SortKey(2, SortOrder.ASCENDING));
                break;
            case "Customer (Z-A)":
                keys.add(new RowSorter.SortKey(2, SortOrder.DESCENDING));
                break;
        }
        sorter.setSortKeys(keys);
    }

    private void updateTotalSales() {
        double total = 0.0;
        int rowCount = table.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            Object value = table.getValueAt(i, 5);
            if (value instanceof Double)
                total += (Double) value;
            else if (value instanceof String) {
                try {
                    total += Double.parseDouble((String) value);
                } catch (Exception e) {
                }
            }
        }
        totalSalesLabel.setText(String.format("Total Sales: RM %.2f", total));
    }

    private void resetFilters() {
        searchField.setText("");
        fromDatePicker.clear();
        toDatePicker.clear();
        filterMethodCombo.setSelectedIndex(0);
        sortCombo.setSelectedIndex(0);
        applyFilters();
        applySorting();
    }

    // =========================================================================
    // PURE JAVA CUSTOM DATE PICKER COMPONENT
    // =========================================================================

    private class ModernDatePicker extends JPanel {
        private JTextField dateField;
        private JButton calendarBtn;
        private List<ActionListener> listeners = new ArrayList<>();
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        public ModernDatePicker() {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(150, 35));
            setBackground(Color.WHITE);
            setBorder(new LineBorder(new Color(200, 200, 200), 1, true));

            dateField = new JTextField();
            dateField.setBorder(new EmptyBorder(0, 8, 0, 0));
            dateField.setFont(new Font("SansSerif", Font.PLAIN, 13));
            dateField.setEditable(false);
            dateField.setBackground(Color.WHITE);

            calendarBtn = new JButton("📅");
            calendarBtn.setFocusPainted(false);
            calendarBtn.setBorderPainted(false);
            calendarBtn.setContentAreaFilled(false);
            calendarBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            calendarBtn.addActionListener(e -> showPopup());

            add(dateField, BorderLayout.CENTER);
            add(calendarBtn, BorderLayout.EAST);
        }

        private void showPopup() {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
            dialog.setUndecorated(true);
            dialog.setSize(260, 260);
            Point p = dateField.getLocationOnScreen();
            dialog.setLocation(p.x, p.y + 35);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBorder(new LineBorder(new Color(105, 108, 255), 1));
            mainPanel.setBackground(Color.WHITE);

            // -- Calendar Logic --
            YearMonth currentYM = YearMonth.now();
            if (!dateField.getText().isEmpty()) {
                try {
                    currentYM = YearMonth.from(LocalDate.parse(dateField.getText(), fmt));
                } catch (Exception ignored) {
                }
            }
            final YearMonth[] viewYM = { currentYM };

            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(240, 248, 255));
            JLabel monthLabel = new JLabel(viewYM[0].getMonth() + " " + viewYM[0].getYear(), SwingConstants.CENTER);
            monthLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

            JButton prev = createNavBtn("<");
            JButton next = createNavBtn(">");

            JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
            grid.setBackground(Color.WHITE);
            grid.setBorder(new EmptyBorder(5, 5, 5, 5));

            Runnable refreshGrid = () -> {
                grid.removeAll();
                monthLabel.setText(viewYM[0].getMonth().toString() + " " + viewYM[0].getYear());

                String[] days = { "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" };
                for (String d : days) {
                    JLabel l = new JLabel(d, SwingConstants.CENTER);
                    l.setFont(new Font("SansSerif", Font.BOLD, 12));
                    l.setForeground(Color.GRAY);
                    grid.add(l);
                }

                LocalDate firstOfMonth = viewYM[0].atDay(1);
                int emptySlots = firstOfMonth.getDayOfWeek().getValue() % 7;
                int daysInMonth = viewYM[0].lengthOfMonth();

                for (int i = 0; i < emptySlots; i++)
                    grid.add(new JLabel(""));

                for (int i = 1; i <= daysInMonth; i++) {
                    final int day = i;
                    JButton dayBtn = new JButton(String.valueOf(day));
                    dayBtn.setFocusPainted(false);
                    dayBtn.setBackground(Color.WHITE);
                    dayBtn.setBorder(new LineBorder(new Color(230, 230, 230)));
                    dayBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    dayBtn.addMouseListener(new MouseAdapter() {
                        public void mouseEntered(MouseEvent e) {
                            dayBtn.setBackground(new Color(230, 240, 255));
                        }

                        public void mouseExited(MouseEvent e) {
                            dayBtn.setBackground(Color.WHITE);
                        }
                    });
                    dayBtn.addActionListener(ev -> {
                        LocalDate selected = viewYM[0].atDay(day);
                        dateField.setText(selected.format(fmt));
                        fireChange();
                        dialog.dispose();
                    });
                    grid.add(dayBtn);
                }
                grid.revalidate();
                grid.repaint();
            };

            prev.addActionListener(e -> {
                viewYM[0] = viewYM[0].minusMonths(1);
                refreshGrid.run();
            });
            next.addActionListener(e -> {
                viewYM[0] = viewYM[0].plusMonths(1);
                refreshGrid.run();
            });

            header.add(prev, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(next, BorderLayout.EAST);

            mainPanel.add(header, BorderLayout.NORTH);
            mainPanel.add(grid, BorderLayout.CENTER);

            // Close if clicked outside (simple simulation)
            dialog.addWindowFocusListener(new WindowAdapter() {
                public void windowLostFocus(WindowEvent e) {
                    dialog.dispose();
                }
            });

            refreshGrid.run();
            dialog.add(mainPanel);
            dialog.setVisible(true);
        }

        private JButton createNavBtn(String text) {
            JButton b = new JButton(text);
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFont(new Font("SansSerif", Font.BOLD, 14));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return b;
        }

        public String getSelectedDate() {
            return dateField.getText().isEmpty() ? null : dateField.getText();
        }

        public void clear() {
            dateField.setText("");
        }

        public void addDateChangeListener(ActionListener l) {
            listeners.add(l);
        }

        private void fireChange() {
            for (ActionListener l : listeners)
                l.actionPerformed(null);
        }
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private void addLiveListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(100, 116, 139));
        return l;
    }

    private void styleTextField(JTextField txt) {
        txt.setPreferredSize(new Dimension(150, 35));
        txt.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txt.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200)), new EmptyBorder(5, 8, 5, 8)));
    }

    private void styleCombo(JComboBox<String> box) {
        box.setBackground(Color.WHITE);
        box.setFont(new Font("SansSerif", Font.PLAIN, 13));
        box.setPreferredSize(new Dimension(120, 35));
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.GRAY);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(Color.LIGHT_GRAY, 1, true));
        btn.setPreferredSize(new Dimension(100, 35));
    }
}