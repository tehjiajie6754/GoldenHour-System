package com.goldenhour.gui.dashboard;

import com.goldenhour.categories.Sales;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.gui.common.Card;
import com.goldenhour.service.loginregister.AuthService;
import com.goldenhour.storage.DatabaseHandler;
import com.goldenhour.gui.dashboard.TopProductPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class HomePanel extends JPanel {

    // Colors
    private final Color COL_PRIMARY = new Color(105, 108, 255); 
    private final Color COL_SUCCESS = new Color(113, 221, 55);  
    private final Color COL_WARNING = new Color(255, 171, 0);   
    private final Color COL_INFO    = new Color(3, 195, 236);   
    private final Color COL_TEXT    = new Color(86, 106, 127);  
    private final Color COL_HEAD    = new Color(50, 71, 92);    

    public HomePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel mainGrid = new JPanel(new GridBagLayout());
        mainGrid.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(mainGrid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);

        // --- DATA ---
        if(DataLoad.allSales == null) DataLoad.allSales = DatabaseHandler.fetchAllSales();
        double totalSalesAmt = DataLoad.allSales.stream().mapToDouble(Sales::getSubtotal).sum();
        
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        long todaySalesCount = DataLoad.allSales.stream().filter(s -> s.getDate().equals(todayStr)).count();
        double todayProfit = DataLoad.allSales.stream().filter(s -> s.getDate().equals(todayStr)).mapToDouble(Sales::getSubtotal).sum();
        int staffCount = DataLoad.allEmployees.size();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        // ROW 1
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; 
        gbc.weightx = 0.5; gbc.weighty = 0.2;
        mainGrid.add(createWelcomeCard(), gbc);

        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0.25;
        mainGrid.add(createStatsWithChartCard("New Sales", "+" + todaySalesCount, "Count Today", COL_SUCCESS, new MiniLineChart(COL_SUCCESS)), gbc);

        gbc.gridx = 3; gbc.gridwidth = 1; gbc.weightx = 0.25;
        mainGrid.add(createStatsWithChartCard("Active Staff", String.valueOf(staffCount), "Employees", COL_WARNING, new MiniBarChart(COL_WARNING)), gbc);

        // ROW 2 (Graph & Side Stats)
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.gridheight = 2; 
        gbc.weightx = 0.75; gbc.weighty = 0.5;
        mainGrid.add(createTotalIncomeCard(), gbc);

        gbc.gridx = 3; gbc.gridy = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
        gbc.weightx = 0.25; gbc.weighty = 0.25;
        mainGrid.add(createStatCard("Profit (Today)", String.format("RM %.2f", todayProfit), "+12% ↗", COL_INFO, "💰"), gbc);

        gbc.gridx = 3; gbc.gridy = 2;
        mainGrid.add(createStatCard("Total Sales", String.format("RM %.2f", totalSalesAmt), "+28% ↗", COL_PRIMARY, "💼"), gbc);

        // ROW 3
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weighty = 0.3;
        mainGrid.add(new TopProductPanel(), gbc); // <--- ADD THIS LINE

        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3; 
        mainGrid.add(createTransactionListCard(), gbc);
    }

    // =========================================================================
    //  CARD CREATION
    // =========================================================================

    private JPanel createTotalIncomeCard() {
        Card card = new Card(Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JPanel titleBox = new JPanel(new GridLayout(2,1)); 
        titleBox.setOpaque(false); 
        JLabel title = new JLabel("Total Income");
        title.setFont(new Font("SansSerif", Font.BOLD, 18)); title.setForeground(COL_HEAD);
        JLabel sub = new JLabel("Report overview");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12)); sub.setForeground(COL_TEXT);
        titleBox.add(title); titleBox.add(sub);
        header.add(titleBox, BorderLayout.WEST);

        // FILTER DROPDOWN
        String[] filters = {"Year", "Month", "Week", "Day"};
        JComboBox<String> filterBox = new JComboBox<>(filters);
        filterBox.setFont(new Font("SansSerif", Font.BOLD, 12));
        filterBox.setBackground(Color.WHITE);
        filterBox.setFocusable(false);
        
        // INTERACTIVE GRAPH
        SmoothGraphPanel graph = new SmoothGraphPanel();
        filterBox.addActionListener(e -> graph.setFilter((String) filterBox.getSelectedItem()));
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterPanel.setOpaque(false);
        filterPanel.add(filterBox);
        header.add(filterPanel, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);
        card.add(graph, BorderLayout.CENTER);
        return card;
    }

    // --- OTHER CARDS (Welcome, Stats, etc.) REMAIN SAME AS PREVIOUS ---
    // (I will condense these for brevity, they are identical to the previous working version)
    
    private JPanel createWelcomeCard() {
        Card card = new Card(Color.WHITE);
        card.setLayout(new BorderLayout());
        JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        textPanel.setOpaque(false); textPanel.setBorder(new EmptyBorder(25, 25, 25, 0));
        String userName = (AuthService.getCurrentUser() != null) ? AuthService.getCurrentUser().getName() : "Manager";
        JLabel title = new JLabel("Welcome back,"); title.setFont(new Font("SansSerif", Font.BOLD, 16)); title.setForeground(COL_PRIMARY);
        JLabel name = new JLabel(userName + "! 👋"); name.setFont(new Font("SansSerif", Font.BOLD, 24)); name.setForeground(COL_HEAD);
        JLabel sub = new JLabel("Here's what's happening today."); sub.setFont(new Font("SansSerif", Font.PLAIN, 13)); sub.setForeground(COL_TEXT);
        textPanel.add(title); textPanel.add(name); textPanel.add(sub);
        JLabel iconLabel = new JLabel("🏆"); iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 70)); iconLabel.setBorder(new EmptyBorder(0, 0, 0, 30));
        card.add(textPanel, BorderLayout.CENTER); card.add(iconLabel, BorderLayout.EAST);
        return card;
    }

    private JPanel createStatsWithChartCard(String title, String value, String sub, Color color, JPanel chart) {
        Card card = new Card(Color.WHITE);
        card.setLayout(new BorderLayout()); card.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel left = new JPanel(new GridLayout(3, 1, 0, 2)); left.setOpaque(false);
        JLabel t = new JLabel(value); t.setFont(new Font("SansSerif", Font.BOLD, 22)); t.setForeground(COL_HEAD);
        JLabel s = new JLabel(title); s.setFont(new Font("SansSerif", Font.BOLD, 13)); s.setForeground(COL_TEXT);
        JLabel subL = new JLabel(sub); subL.setFont(new Font("SansSerif", Font.PLAIN, 11)); subL.setForeground(Color.LIGHT_GRAY);
        left.add(t); left.add(s); left.add(subL);
        card.add(left, BorderLayout.CENTER); card.add(chart, BorderLayout.EAST);
        return card;
    }

    private JPanel createStatCard(String title, String value, String change, Color iconBg, String icon) {
        Card card = new Card(Color.WHITE);
        card.setLayout(new BorderLayout()); card.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel iconBox = new Card(new Color(iconBg.getRed(), iconBg.getGreen(), iconBg.getBlue(), 40));
        iconBox.setPreferredSize(new Dimension(45, 45));
        JLabel il = new JLabel(icon); il.setFont(new Font("SansSerif", Font.PLAIN, 20)); iconBox.add(il);
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); header.setOpaque(false); header.add(iconBox);
        JPanel content = new JPanel(new GridLayout(3, 1, 0, 2)); content.setOpaque(false); content.setBorder(new EmptyBorder(15, 0, 0, 0));
        JLabel t = new JLabel(title); t.setFont(new Font("SansSerif", Font.BOLD, 13)); t.setForeground(COL_TEXT);
        JLabel v = new JLabel(value); v.setFont(new Font("SansSerif", Font.BOLD, 20)); v.setForeground(COL_HEAD);
        JLabel c = new JLabel(change); c.setFont(new Font("SansSerif", Font.BOLD, 12)); c.setForeground(COL_SUCCESS);
        content.add(t); content.add(v); content.add(c);
        card.add(header, BorderLayout.NORTH); card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTransactionListCard() {
        Card card = new Card(Color.WHITE);
        card.setLayout(new BorderLayout()); card.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel t = new JLabel("Recent Transactions"); t.setFont(new Font("SansSerif", Font.BOLD, 16)); t.setForeground(COL_HEAD);
        card.add(t, BorderLayout.NORTH);
        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setOpaque(false); list.setBorder(new EmptyBorder(10,0,0,0));
        List<Sales> recent = DataLoad.allSales.stream().sorted((s1, s2) -> s2.getDate().compareTo(s1.getDate())).limit(3).collect(Collectors.toList());
        for(Sales s : recent) {
            String method = s.getTransactionMethod();
            Color iconBg = method.equalsIgnoreCase("Cash") ? new Color(255, 235, 235) : new Color(235, 255, 235);
            Color iconFg = method.equalsIgnoreCase("Cash") ? COL_WARNING : COL_SUCCESS;
            list.add(createTransactionItem(method, s.getDate(), "+RM " + String.format("%.2f", s.getSubtotal()), iconBg, iconFg));
            list.add(Box.createVerticalStrut(15));
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTransactionItem(String title, String sub, String amt, Color bg, Color fg) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        JPanel icon = new Card(bg); icon.setPreferredSize(new Dimension(40, 40));
        JLabel l = new JLabel(title.substring(0,1)); l.setForeground(fg); l.setFont(new Font("SansSerif", Font.BOLD, 16)); icon.add(l);
        JPanel text = new JPanel(new GridLayout(2, 1)); text.setOpaque(false); text.setBorder(new EmptyBorder(0, 15, 0, 0));
        JLabel t = new JLabel(title); t.setFont(new Font("SansSerif", Font.BOLD, 14)); t.setForeground(COL_HEAD);
        JLabel s = new JLabel(sub); s.setFont(new Font("SansSerif", Font.PLAIN, 12)); s.setForeground(COL_TEXT);
        text.add(t); text.add(s);
        JLabel a = new JLabel(amt); a.setFont(new Font("SansSerif", Font.BOLD, 14)); a.setForeground(COL_SUCCESS);
        p.add(icon, BorderLayout.WEST); p.add(text, BorderLayout.CENTER); p.add(a, BorderLayout.EAST);
        return p;
    }

    // --- MINI CHARTS ---
    private static class MiniLineChart extends JPanel {
        private Color color; public MiniLineChart(Color c) { this.color = c; setOpaque(false); setPreferredSize(new Dimension(60, 60)); }
        protected void paintComponent(Graphics g) { super.paintComponent(g); Graphics2D g2 = (Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); int w=getWidth(), h=getHeight(); int[] vals={20,10,30,25,40,30,50}; Path2D.Float p=new Path2D.Float(); int step=w/(vals.length-1); p.moveTo(0, h-vals[0]); for(int i=1; i<vals.length; i++) p.lineTo(i*step, h-vals[i]); g2.setColor(color); g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g2.draw(p); }
    }
    private static class MiniBarChart extends JPanel {
        private Color color; public MiniBarChart(Color c) { this.color = c; setOpaque(false); setPreferredSize(new Dimension(60, 60)); }
        protected void paintComponent(Graphics g) { super.paintComponent(g); Graphics2D g2 = (Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); int w=getWidth(), h=getHeight(); int bars=5; int gap=4; int bw=(w-(gap*(bars-1)))/bars; int[] vals={30,50,20,40,35}; g2.setColor(color); for(int i=0; i<bars; i++) { int bh=vals[i]; g2.fillRoundRect(i*(bw+gap), h-bh, bw, bh, 4, 4); } }
    }

    // =========================================================================
    //  ADVANCED INTERACTIVE GRAPH ENGINE
    // =========================================================================

    // =========================================================================
    //  ADVANCED INTERACTIVE GRAPH ENGINE (Fixed Week/Day Logic)
    // =========================================================================

    private class SmoothGraphPanel extends JPanel {
        private String currentFilter = "Year";
        private final List<Point2D.Double> points = new ArrayList<>();
        private final List<String> labels = new ArrayList<>();
        private final Object dataLock = new Object();
        
        private double zoom = 1.0;
        private double xOffset = 0;
        
        // Layout Constants
        private final int LEFT_MARGIN = 50; 
        private final int BOTTOM_PAD = 30;
        private final int TOP_PAD = 20;
        
        private Point mousePt = null;
        private Point2D.Double hoveredDataPoint = null;
        private String hoveredLabel = null;
        
        public SmoothGraphPanel() {
            setOpaque(false);
            
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePt = e.getPoint();
                    findHoveredPoint();
                    repaint();
                }
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (zoom > 1.0) {
                        double dx = e.getX() - mousePt.x;
                        xOffset += dx;
                        int graphW = getWidth() - LEFT_MARGIN;
                        double maxOffset = 0;
                        double minOffset = -(graphW * zoom - graphW);
                        if(xOffset > maxOffset) xOffset = maxOffset;
                        if(xOffset < minOffset) xOffset = minOffset;
                        mousePt = e.getPoint();
                        repaint();
                    }
                }
            };
            addMouseMotionListener(ma);
            addMouseListener(ma);

            addMouseWheelListener(e -> {
                double oldZoom = zoom;
                if (e.getWheelRotation() < 0) zoom *= 1.1; else zoom /= 1.1; 
                if (zoom < 1.0) zoom = 1.0; if (zoom > 10.0) zoom = 10.0;
                double mouseX = e.getX() - LEFT_MARGIN; 
                xOffset = mouseX - (mouseX - xOffset) * (zoom / oldZoom);
                repaint();
            });
            refreshData();
        }

        public void setFilter(String filter) {
            this.currentFilter = filter;
            this.zoom = 1.0;
            this.xOffset = 0;
            refreshData();
            repaint();
        }

        private void refreshData() {
            synchronized (dataLock) {
                points.clear();
                labels.clear();
                Map<String, Double> aggData = new LinkedHashMap<>();
                LocalDate now = LocalDate.now();
                if (DataLoad.allSales == null) DataLoad.allSales = new ArrayList<>();

                // =============================================================
                //  1. YEAR VIEW (Jan - Dec)
                // =============================================================
                if (currentFilter.equals("Year")) {
                    for(int i=1; i<=12; i++) aggData.put(String.valueOf(i), 0.0);
                    for (Sales s : DataLoad.allSales) {
                        try {
                            LocalDate d = LocalDate.parse(s.getDate());
                            if (d.getYear() == now.getYear()) 
                                aggData.merge(String.valueOf(d.getMonthValue()), s.getSubtotal(), Double::sum);
                        } catch (Exception ignored) {}
                    }
                    int idx = 0;
                    for(Map.Entry<String, Double> e : aggData.entrySet()) {
                        points.add(new Point2D.Double(idx++, e.getValue()));
                        labels.add(java.time.Month.of(Integer.parseInt(e.getKey())).name().substring(0,3));
                    }
                } 
                
                // =============================================================
                //  2. MONTH VIEW (1st - 31st)
                // =============================================================
                else if (currentFilter.equals("Month")) {
                    int days = now.lengthOfMonth();
                    for(int i=1; i<=days; i++) aggData.put(String.valueOf(i), 0.0);
                    for (Sales s : DataLoad.allSales) {
                        try {
                            LocalDate d = LocalDate.parse(s.getDate());
                            if (d.getYear() == now.getYear() && d.getMonth() == now.getMonth()) 
                                aggData.merge(String.valueOf(d.getDayOfMonth()), s.getSubtotal(), Double::sum);
                        } catch (Exception ignored) {}
                    }
                    int idx = 0;
                    for(Map.Entry<String, Double> e : aggData.entrySet()) {
                        points.add(new Point2D.Double(idx++, e.getValue()));
                        labels.add(e.getKey());
                    }
                } 
                
                // =============================================================
                //  3. WEEK VIEW (Monday - Sunday of Current Week)
                // =============================================================
                else if (currentFilter.equals("Week")) {
                    String[] days = {"MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"};
                    String[] shortDays = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
                    for(String d : days) aggData.put(d, 0.0);
                    
                    // Robust Logic: Find start (Mon) and end (Sun) of current week
                    LocalDate startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                    LocalDate endOfWeek = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

                    for (Sales s : DataLoad.allSales) {
                        try {
                            LocalDate d = LocalDate.parse(s.getDate());
                            // Check if date falls within this week range (Inclusive)
                            if (!d.isBefore(startOfWeek) && !d.isAfter(endOfWeek)) {
                                aggData.merge(d.getDayOfWeek().name(), s.getSubtotal(), Double::sum);
                            }
                        } catch (Exception ignored) {}
                    }
                    int idx = 0;
                    for(int i=0; i<days.length; i++) {
                        points.add(new Point2D.Double(idx++, aggData.get(days[i])));
                        labels.add(shortDays[i]);
                    }
                } 
                
                // =============================================================
                //  4. DAY VIEW (Hourly 00:00 - 23:00)
                // =============================================================
                else if (currentFilter.equals("Day")) {
                    for(int i=0; i<24; i++) aggData.put(String.format("%02d", i), 0.0);
                    
                    for (Sales s : DataLoad.allSales) {
                        if (s.getDate().equals(now.toString())) {
                            try {
                                String time = s.getTime().trim().toLowerCase(); // "11:25 am", "06:27 pm"
                                // Extract hour
                                String[] parts = time.split("[: ]"); 
                                int hour = Integer.parseInt(parts[0]);
                                
                                // Handle AM/PM
                                boolean isPM = time.contains("pm") || time.contains("p.m.");
                                boolean isAM = time.contains("am") || time.contains("a.m.");
                                
                                if (isPM && hour < 12) hour += 12;
                                if (isAM && hour == 12) hour = 0; // Midnight case
                                
                                aggData.merge(String.format("%02d", hour), s.getSubtotal(), Double::sum);
                            } catch(Exception ignored){
                                // System.out.println("Time parse error: " + s.getTime()); // Debug
                            }
                        }
                    }
                    int idx = 0;
                    for(Map.Entry<String, Double> e : aggData.entrySet()) {
                        points.add(new Point2D.Double(idx++, e.getValue()));
                        labels.add(e.getKey() + ":00");
                    }
                }

                // Fallback
                if(points.isEmpty()) {
                    points.add(new Point2D.Double(0, 0)); labels.add("");
                    points.add(new Point2D.Double(1, 0)); labels.add("");
                }
            }
        }

        private void findHoveredPoint() {
            synchronized (dataLock) {
                hoveredDataPoint = null;
                if(mousePt == null || points.isEmpty()) return;
                if (mousePt.x < LEFT_MARGIN) return;

                int w = getWidth() - LEFT_MARGIN;
                int h = getHeight() - TOP_PAD - BOTTOM_PAD;
                double maxVal = points.stream().mapToDouble(p -> p.y).max().orElse(100.0);
                if(maxVal == 0) maxVal = 100;

                double stepX = w / (double)(points.size() - 1) * zoom;

                for (int i = 0; i < points.size(); i++) {
                    double px = LEFT_MARGIN + (i * stepX) + xOffset;
                    double py = TOP_PAD + h - (points.get(i).y / maxVal) * h;
                    if (Math.abs(mousePt.x - px) < 15) { 
                        hoveredDataPoint = new Point2D.Double(px, py);
                        hoveredLabel = "RM " + String.format("%.2f", points.get(i).y);
                        return;
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int graphH = h - TOP_PAD - BOTTOM_PAD;
            int graphW = w - LEFT_MARGIN;

            List<Point2D.Double> renderPoints;
            List<String> renderLabels;
            Point2D.Double renderHoverPoint;
            String renderHoverLabel;

            synchronized (dataLock) {
                if (points.isEmpty()) return;
                renderPoints = new ArrayList<>(points);
                renderLabels = new ArrayList<>(labels);
                renderHoverPoint = hoveredDataPoint;
                renderHoverLabel = hoveredLabel;
            }

            double maxVal = renderPoints.stream().mapToDouble(p -> p.y).max().orElse(100.0);
            if(maxVal == 0) maxVal = 100;

            // 1. BACKGROUND GRID
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.setStroke(new BasicStroke(1f));
            int steps = 4;
            for (int i = 0; i <= steps; i++) {
                int y = TOP_PAD + (graphH * i / steps);
                g2.setColor(new Color(230, 230, 245));
                g2.drawLine(LEFT_MARGIN, y, w, y);
                g2.setColor(Color.GRAY);
                double val = maxVal * (steps - i) / steps;
                String label = (val >= 1000) ? String.format("%.1fk", val / 1000) : String.format("%.0f", val);
                int strW = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, LEFT_MARGIN - strW - 10, y + 4);
            }

            // 2. SCROLLABLE GRAPH
            Shape originalClip = g2.getClip();
            g2.setClip(LEFT_MARGIN, 0, graphW, h);

            if (!renderPoints.isEmpty()) {
                Path2D.Float path = new Path2D.Float();
                double stepX = (double) graphW / (renderPoints.size() - 1) * zoom;
                double startX = LEFT_MARGIN + xOffset;
                double startY = TOP_PAD + graphH - (renderPoints.get(0).y / maxVal) * graphH;
                path.moveTo(startX, startY);

                for (int i = 1; i < renderPoints.size(); i++) {
                    double px = LEFT_MARGIN + (i * stepX) + xOffset;
                    double py = TOP_PAD + graphH - (renderPoints.get(i).y / maxVal) * graphH;
                    path.lineTo(px, py);
                }
                g2.setColor(COL_PRIMARY);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(path);

                Path2D.Float fillPath = (Path2D.Float) path.clone();
                double lastX = LEFT_MARGIN + ((renderPoints.size()-1) * stepX) + xOffset;
                fillPath.lineTo(lastX, TOP_PAD + graphH);
                fillPath.lineTo(startX, TOP_PAD + graphH);
                fillPath.closePath();
                g2.setPaint(new GradientPaint(0, 0, new Color(105, 108, 255, 80), 0, h, new Color(255, 255, 255, 0)));
                g2.fill(fillPath);

                g2.setColor(Color.GRAY);
                for (int i = 0; i < renderPoints.size(); i++) {
                    double px = LEFT_MARGIN + (i * stepX) + xOffset;
                    if (px >= LEFT_MARGIN - 20 && px <= w + 20) {
                        if (zoom < 1.5 && renderPoints.size() > 15 && i % 2 != 0) continue;
                        String l = renderLabels.get(i);
                        int lw = g2.getFontMetrics().stringWidth(l);
                        g2.drawString(l, (int)px - lw/2, h - 10);
                    }
                }
            }

            // 3. SMART TOOLTIP
            if (renderHoverPoint != null) {
                int hx = (int) renderHoverPoint.x;
                int hy = (int) renderHoverPoint.y;
                g2.setColor(Color.WHITE); g2.fillOval(hx - 6, hy - 6, 12, 12);
                g2.setColor(COL_PRIMARY); g2.setStroke(new BasicStroke(2f)); g2.drawOval(hx - 6, hy - 6, 12, 12);

                String text = renderHoverLabel;
                int tw = g2.getFontMetrics().stringWidth(text) + 20;
                int th = 28;
                int tx = hx - tw / 2;
                int ty = hy - 40; 
                boolean drawBelow = false;

                if (tx < LEFT_MARGIN) tx = LEFT_MARGIN;
                if (tx + tw > w) tx = w - tw;
                if (ty < 5) { ty = hy + 20; drawBelow = true; }

                g2.setColor(new Color(50, 50, 60));
                g2.fillRoundRect(tx, ty, tw, th, 6, 6);
                
                int[] polyX = {hx, hx-5, hx+5};
                int[] polyY = drawBelow ? new int[]{hy + 12, hy + 20, hy + 20} : new int[]{hy - 12, hy - 20, hy - 20};
                g2.fillPolygon(polyX, polyY, 3);

                g2.setColor(Color.WHITE);
                g2.drawString(text, tx + 10, ty + 18);
            }
            g2.setClip(originalClip);
        }
    }
}