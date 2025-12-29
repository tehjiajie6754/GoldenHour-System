package com.goldenhour.gui.inventory;

import com.goldenhour.categories.Model;
import com.goldenhour.categories.Outlet;
import com.goldenhour.gui.common.BackgroundPanel;
import com.goldenhour.gui.common.Card;
import com.goldenhour.storage.DatabaseHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class StockPanel extends BackgroundPanel {

    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> outletCombo;
    private JTextField searchField;
    private JLabel totalValueLabel, lowStockLabel, totalItemsLabel;

    public StockPanel() {
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        // === 1. TOP METRICS ROW ===
        JPanel metricsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        metricsPanel.setOpaque(false);
        
        totalValueLabel = new JLabel("RM 0.00");
        lowStockLabel = new JLabel("0");
        totalItemsLabel = new JLabel("0");
        
        metricsPanel.add(createMetricCard("Total Inventory Value", totalValueLabel, "image_1a786e.png", new Color(111, 66, 193))); 
        metricsPanel.add(createMetricCard("Low Stock Items", lowStockLabel, "", new Color(255, 171, 0))); 
        metricsPanel.add(createMetricCard("Total Models", totalItemsLabel, "", new Color(40, 199, 111))); 
        
        add(metricsPanel, BorderLayout.NORTH);

        // === 2. MAIN CONTENT (Filter + Table) ===
        Card mainCard = new Card(Color.WHITE);
        mainCard.setLayout(new BorderLayout());
        mainCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        // -- Filter Bar --
        JPanel filterBar = new JPanel(new BorderLayout());
        filterBar.setOpaque(false);
        filterBar.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Left: Outlet Dropdown
        JPanel leftFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftFilter.setOpaque(false);
        leftFilter.add(new JLabel("<html><b>Filter:</b></html>"));
        
        outletCombo = new JComboBox<>();
        outletCombo.setBackground(Color.WHITE);
        outletCombo.setPreferredSize(new Dimension(150, 35));
        
        // Initial load
        loadOutlets(); 
        
        outletCombo.addActionListener(e -> refreshData());
        leftFilter.add(outletCombo);
        
        filterBar.add(leftFilter, BorderLayout.WEST);

        // Right: Search + Add Button
        JPanel rightFilter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightFilter.setOpaque(false);
        
        searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(200, 35));
        searchField.putClientProperty("JTextField.placeholderText", "Search Product..."); 
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)), 
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { refreshData(); }
        });
        rightFilter.add(searchField);

        JButton addBtn = new JButton("+ Add Product");
        addBtn.setBackground(new Color(111, 66, 193));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        addBtn.setPreferredSize(new Dimension(120, 35));
        rightFilter.add(addBtn);

        filterBar.add(rightFilter, BorderLayout.EAST);
        mainCard.add(filterBar, BorderLayout.NORTH);

        // -- Modern Table --
        String[] cols = {"PRODUCT", "PRICE", "STOCK QTY", "STATUS", "ACTION"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 250, 251)); 
                }
                return c;
            }
        };

        table.setRowHeight(60);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(240, 240, 255));
        table.setSelectionForeground(Color.BLACK);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JTableHeader th = table.getTableHeader();
        th.setDefaultRenderer(new SimpleHeaderRenderer());
        th.setPreferredSize(new Dimension(0, 40));
        th.setBackground(Color.WHITE);

        table.getColumnModel().getColumn(0).setCellRenderer(new ProductRenderer()); 
        table.getColumnModel().getColumn(3).setCellRenderer(new StockStatusRenderer()); 
        table.getColumnModel().getColumn(4).setCellRenderer(new ActionRenderer()); 

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        mainCard.add(scroll, BorderLayout.CENTER);

        add(mainCard, BorderLayout.CENTER);

        // === 3. REAL-TIME TRIGGER ===
        // This makes the panel refresh every time you click the tab
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadOutlets(); // Refresh dropdown in case new outlets added
                refreshData(); // Refresh table data from DB
            }
        });

        // Initial Load
        refreshData();
    }

    // --- HELPER UI METHODS ---

    private JPanel createMetricCard(String title, JLabel valLabel, String iconPath, Color accent) {
        Card card = new Card(Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setForeground(Color.GRAY);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        top.add(t, BorderLayout.WEST);
        
        JLabel icon = new JLabel("📦"); 
        icon.setForeground(accent);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 20));
        top.add(icon, BorderLayout.EAST);
        
        card.add(top, BorderLayout.NORTH);

        valLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valLabel.setForeground(new Color(52, 71, 103));
        card.add(valLabel, BorderLayout.CENTER);

        return card;
    }

    // --- LOGIC (UPDATED FOR REAL-TIME DB) ---

    private void loadOutlets() {
        // Keep current selection if possible
        Object current = outletCombo.getSelectedItem();
        
        outletCombo.removeAllItems();
        
        // FETCH FROM DB DIRECTLY
        List<Outlet> dbOutlets = DatabaseHandler.fetchAllOutlets();
        
        for (Outlet o : dbOutlets) {
            outletCombo.addItem(o.getOutletCode());
        }
        
        if (outletCombo.getItemCount() == 0) outletCombo.addItem("C60");
        
        // Restore selection
        if (current != null) {
            for(int i=0; i<outletCombo.getItemCount(); i++) {
                if(outletCombo.getItemAt(i).equals(current)) {
                    outletCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void refreshData() {
        model.setRowCount(0);
        
        // 1. FETCH FRESH DATA FROM DB (Real-Time)
        List<Model> freshModels = DatabaseHandler.fetchAllModelsWithStock();
        
        String selectedOutlet = (String) outletCombo.getSelectedItem();
        String keyword = searchField.getText().trim().toLowerCase();
        
        if (selectedOutlet == null) return;

        double totalVal = 0;
        int lowStockCount = 0;
        int totalItems = 0;

        for (Model m : freshModels) {
            // Filter
            if (!keyword.isEmpty() && !m.getModelCode().toLowerCase().contains(keyword)) continue;

            int qty = m.getStock(selectedOutlet);
            
            // Metrics Calculation
            totalVal += (m.getPrice() * qty);
            if (qty < 5) lowStockCount++;
            totalItems++;

            // Status Logic
            String status = "In Stock";
            if (qty == 0) status = "Out of Stock";
            else if (qty < 5) status = "Low Stock";

            model.addRow(new Object[]{
                m.getModelCode(), 
                String.format("RM %.2f", m.getPrice()),
                qty,
                status,
                "" 
            });
        }

        // Update Top Cards
        totalValueLabel.setText(String.format("RM %.2f", totalVal));
        lowStockLabel.setText(String.valueOf(lowStockCount));
        totalItemsLabel.setText(String.valueOf(totalItems));
    }

    // --- CUSTOM RENDERERS (Unchanged) ---

    class SimpleHeaderRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setFont(new Font("SansSerif", Font.BOLD, 11));
            l.setForeground(new Color(160, 174, 192));
            l.setBackground(Color.WHITE);
            l.setBorder(new EmptyBorder(0, 10, 5, 0));
            return l;
        }
    }

    class ProductRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            p.setOpaque(false);
            
            JLabel icon = new JLabel("\uD83D\uDC5C"); 
            icon.setFont(new Font("SansSerif", Font.PLAIN, 24));
            icon.setForeground(new Color(100, 100, 100));
            
            JLabel text = new JLabel("<html><b>" + value + "</b><br><font color='gray' size='2'>Watches</font></html>");
            
            p.add(icon);
            p.add(text);
            return p;
        }
    }

    class StockStatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
            p.setOpaque(false);
            
            String status = (String) value;
            Color bg = new Color(213, 245, 227); 
            Color fg = new Color(40, 199, 111);  
            
            if (status.equals("Low Stock")) {
                bg = new Color(255, 242, 226); 
                fg = new Color(255, 171, 0);   
            } else if (status.equals("Out of Stock")) {
                bg = new Color(255, 226, 229); 
                fg = new Color(234, 84, 85);   
            }

            JLabel lbl = new JLabel(status) {
                protected void paintComponent(Graphics g) {
                    g.setColor(getBackground());
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    super.paintComponent(g);
                }
            };
            lbl.setOpaque(false);
            lbl.setBackground(bg);
            lbl.setForeground(fg);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setPreferredSize(new Dimension(85, 22));
            
            p.add(lbl);
            return p;
        }
    }

    class ActionRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            p.setOpaque(false);
            
            JLabel edit = new JLabel("\u270E"); 
            edit.setFont(new Font("SansSerif", Font.BOLD, 16));
            edit.setForeground(Color.GRAY);
            edit.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JLabel del = new JLabel("\uD83D\uDDD1"); 
            del.setFont(new Font("SansSerif", Font.BOLD, 16));
            del.setForeground(new Color(234, 84, 85));
            del.setCursor(new Cursor(Cursor.HAND_CURSOR));

            p.add(edit);
            p.add(del);
            return p;
        }
    }
}