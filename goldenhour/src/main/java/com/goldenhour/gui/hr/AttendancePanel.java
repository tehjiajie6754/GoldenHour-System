package com.goldenhour.gui.hr;

import com.goldenhour.categories.Attendance;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.gui.common.BackgroundPanel;
import com.goldenhour.gui.common.Card;
import com.goldenhour.service.loginregister.AuthService;
import com.goldenhour.storage.CSVHandler;
import com.goldenhour.storage.DatabaseHandler;
import com.goldenhour.utils.TimeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class AttendancePanel extends BackgroundPanel { // Inherits background image

    private JTable table;
    private DefaultTableModel model;
    private JLabel statusLabel;
    private JButton clockInBtn, clockOutBtn;

    public AttendancePanel() {
        setLayout(new BorderLayout());
        // No setBackground(...) here, handled by BackgroundPanel
        setBorder(new EmptyBorder(30, 30, 30, 30));

        // === 1. TOP HEADER ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false); // Transparent header container
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Attendance Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(52, 71, 103));
        
        statusLabel = new JLabel("Current Status: Checking...");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        statusLabel.setForeground(Color.GRAY);
        
        titleBox.add(title);
        titleBox.add(statusLabel);
        headerPanel.add(titleBox, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        btnPanel.setOpaque(false);

        clockInBtn = createActionBtn("Clock In", new Color(40, 199, 111));
        clockOutBtn = createActionBtn("Clock Out", new Color(234, 84, 85));
        
        clockInBtn.addActionListener(e -> performClockIn());
        clockOutBtn.addActionListener(e -> performClockOut());

        btnPanel.add(clockInBtn);
        btnPanel.add(clockOutBtn);
        headerPanel.add(btnPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // === 2. GLASS TABLE CARD ===
        // Use a semi-transparent white (Alpha = 200) for the card background
        Card tableCard = new Card(new Color(255, 255, 255, 200));
        tableCard.setLayout(new BorderLayout());
        
        // Table Data
        String[] cols = {"DATE", "CLOCK IN", "CLOCK OUT", "TOTAL HOURS", "STATUS"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    // GLASS EFFECT STRIPES
                    // Even rows: Very transparent white (30 alpha)
                    // Odd rows: Slightly more opaque white (80 alpha)
                    c.setBackground(row % 2 == 0 ? new Color(255, 255, 255, 30) : new Color(255, 255, 255, 120));
                } else {
                    c.setBackground(new Color(105, 108, 255, 100)); // Selection is purple glass
                }
                return c;
            }
        };

        // Transparency Settings
        table.setOpaque(false); // Make table body transparent
        ((DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)).setOpaque(false); // Cells transparent
        table.setShowGrid(false);
        table.setRowHeight(55);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // Header Styling
        JTableHeader th = table.getTableHeader();
        th.setDefaultRenderer(new SimpleHeaderRenderer());
        th.setPreferredSize(new Dimension(0, 50));
        th.setBackground(new Color(255, 255, 255, 150)); // Semi-transparent header

        // Column Alignments
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(JLabel.CENTER);
        centerRender.setOpaque(false); // Critical for glass effect

        table.getColumnModel().getColumn(0).setCellRenderer(new PaddingRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(centerRender);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRender);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRender);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());

        // Scroll Pane Transparency
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false); // Transparent container
        scroll.getViewport().setOpaque(false); // Transparent viewport
        
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        refreshData();
    }

    private JButton createActionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(140, 45));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // --- RENDERERS ---
    class SimpleHeaderRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setFont(new Font("SansSerif", Font.BOLD, 12));
            l.setForeground(new Color(100, 110, 140));
            // Semi-transparent header background
            l.setBackground(new Color(255, 255, 255, 100)); 
            l.setOpaque(false); // Let the paintComponent handle the alpha color
            l.setBorder(new EmptyBorder(0, 0, 10, 0));
            l.setHorizontalAlignment(column == 0 ? JLabel.LEFT : JLabel.CENTER);
            if(column==0) l.setBorder(new EmptyBorder(0, 20, 10, 0));
            return l;
        }
        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0,0,getWidth(),getHeight());
            super.paintComponent(g);
        }
    }

    class PaddingRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 20, 0, 0));
            setOpaque(false); // Critical
            return this;
        }
    }

    class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
            panel.setOpaque(false); // Panel transparent

            String status = (String) value;
            Color bgColor, textColor;

            if ("Active".equals(status)) {
                bgColor = new Color(232, 217, 255, 220); // Semi-transparent Purple
                textColor = new Color(111, 66, 193);
            } else {
                bgColor = new Color(213, 245, 227, 220); // Semi-transparent Green
                textColor = new Color(40, 199, 111);
            }

            JLabel lbl = new JLabel(status) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bgColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    super.paintComponent(g);
                }
            };
            lbl.setOpaque(false);
            lbl.setForeground(textColor);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setPreferredSize(new Dimension(90, 25));

            panel.add(lbl);
            return panel;
        }
    }

    // --- DATA LOGIC (Unchanged) ---
    private void refreshData() {
        model.setRowCount(0);
        String userId = AuthService.getCurrentUser().getId();
        List<Attendance> history = DataLoad.allAttendance.stream()
                .filter(a -> a.getEmployeeId().equals(userId))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());

        Attendance todayRec = null;
        String today = TimeUtils.getDate();

        for (Attendance a : history) {
            String status = (a.getClockOutTime() == null || a.getClockOutTime().isEmpty()) ? "Active" : "Completed";
            model.addRow(new Object[]{
                a.getDate(),
                a.getClockInTime(),
                (a.getClockOutTime() == null ? "-" : a.getClockOutTime()),
                String.format("%.1f hrs", a.getHoursWorked()),
                status
            });
            if(a.getDate().equals(today)) todayRec = a;
        }

        if (todayRec == null) {
            statusLabel.setText("You haven't clocked in today.");
            clockInBtn.setEnabled(true); clockInBtn.setBackground(new Color(40, 199, 111));
            clockOutBtn.setEnabled(false); clockOutBtn.setBackground(new Color(200,200,200));
        } else if (todayRec.getClockOutTime() == null) {
            statusLabel.setText("You are currently Working.");
            clockInBtn.setEnabled(false); clockInBtn.setBackground(new Color(200,200,200));
            clockOutBtn.setEnabled(true); clockOutBtn.setBackground(new Color(234, 84, 85));
        } else {
            statusLabel.setText("Shift Completed (" + String.format("%.1f", todayRec.getHoursWorked()) + " hrs)");
            clockInBtn.setEnabled(false); clockInBtn.setBackground(new Color(200,200,200));
            clockOutBtn.setEnabled(false); clockOutBtn.setBackground(new Color(200,200,200));
        }
    }

    private void performClockIn() {
        // Simple logic for brevity, matches previous
        Attendance newRecord = new Attendance(AuthService.getCurrentUser().getId(), AuthService.getCurrentUser().getName(), TimeUtils.getDate(), TimeUtils.getTime());
        DataLoad.allAttendance.add(newRecord);
        DatabaseHandler.saveAttendance(newRecord);
        CSVHandler.writeAttendance(DataLoad.allAttendance);
        JOptionPane.showMessageDialog(this, "Clocked In Successfully!");
        refreshData();
    }

    private void performClockOut() {
        String today = TimeUtils.getDate();
        Attendance todayRecord = DataLoad.allAttendance.stream()
                .filter(a -> a.getEmployeeId().equals(AuthService.getCurrentUser().getId()) && a.getDate().equals(today))
                .findFirst().orElse(null);
        if (todayRecord != null) {
            todayRecord.setClockOutTime(TimeUtils.getTime());
            todayRecord.setHoursWorked(calculateHours(todayRecord.getClockInTime(), todayRecord.getClockOutTime()));
            DatabaseHandler.updateAttendance(todayRecord);
            CSVHandler.writeAttendance(DataLoad.allAttendance);
            JOptionPane.showMessageDialog(this, "Clocked Out!");
            refreshData();
        }
    }

    private double calculateHours(String in, String out) {
        try {
            long min = ChronoUnit.MINUTES.between(
                LocalDateTime.parse("2000-01-01 " + in, DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")),
                LocalDateTime.parse("2000-01-01 " + out, DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"))
            );
            return min / 60.0;
        } catch (Exception e) { return 0.0; }
    }
}