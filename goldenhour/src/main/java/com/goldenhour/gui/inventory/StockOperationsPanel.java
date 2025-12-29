package com.goldenhour.gui.inventory;

import com.goldenhour.categories.Model;
import com.goldenhour.categories.Outlet;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.gui.common.BackgroundPanel;
import com.goldenhour.gui.common.Card;
import com.goldenhour.storage.DatabaseHandler;
import com.goldenhour.storage.ReceiptHandler; // Ensure this is imported
import com.goldenhour.service.loginregister.AuthService; // Ensure this is imported

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StockOperationsPanel extends BackgroundPanel {

    // --- MAIN NAVIGATION STATE ---
    private enum MainTab { MOVEMENT, COUNT }
    private MainTab currentMainTab = MainTab.MOVEMENT;

    // --- MOVEMENT STATE ---
    private enum MoveType { STOCK_IN, STOCK_OUT }
    private MoveType currentMoveType = MoveType.STOCK_IN;

    // --- COUNT STATE ---
    private enum CountStep { SELECTION, INPUT, REPORT }
    private enum CountType { MORNING, NIGHT }
    private CountStep currentCountStep = CountStep.SELECTION;
    //private CountType currentCountType = CountType.MORNING;

    // --- UI COMPONENTS ---
    private JPanel tabsContainer;
    private JPanel bodyPanel;
    private CardLayout bodyLayout;

    // 1. MOVEMENT PANELS
    private JPanel movementPanel;
    private JPanel moveCardsPanel;
    private JPanel moveFormPanel;
    private JComboBox<String> sourceCombo, destCombo;
    private JTextField modelField, qtyField;
    
    // Components for "Cart"
    private JTable transferCartTable;
    private DefaultTableModel transferCartModel;
    private JButton executeBatchBtn;
    private JButton addToCartBtn;

    // 2. COUNT PANELS
    private JPanel countPanel;
    private ProgressBarPanel countProgressBar;
    private JPanel countContentPanel; 
    private CardLayout countCardLayout;
    private JComboBox<String> countOutletSelector;
    private JTable countInputTable;
    private DefaultTableModel countInputModel;
    private StockReportPanel reportPanel;

    public StockOperationsPanel() {
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // === A. HEADER ===
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Stock Operations");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(new Color(52, 71, 103));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        tabsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 0));
        tabsContainer.setOpaque(false);
        tabsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        header.add(title);
        header.add(Box.createVerticalStrut(20));
        header.add(tabsContainer);
        header.add(Box.createVerticalStrut(10));
        
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(230, 230, 230));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(sep);

        add(header, BorderLayout.NORTH);

        // === B. BODY ===
        bodyLayout = new CardLayout();
        bodyPanel = new JPanel(bodyLayout);
        bodyPanel.setOpaque(false);

        initMovementUI();
        initCountUI();

        bodyPanel.add(movementPanel, "MOVEMENT");
        bodyPanel.add(countPanel, "COUNT");

        add(bodyPanel, BorderLayout.CENTER);
        renderMainTabs();
    }

    // =========================================================================
    //  SECTION 1: MAIN NAVIGATION
    // =========================================================================

    private void renderMainTabs() {
        tabsContainer.removeAll();
        tabsContainer.add(createTabButton("Stock Movement", MainTab.MOVEMENT));
        tabsContainer.add(createTabButton("Stock Count", MainTab.COUNT));
        tabsContainer.revalidate();
        tabsContainer.repaint();
    }

    private JLabel createTabButton(String text, MainTab tabType) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (currentMainTab == tabType) {
            lbl.setForeground(new Color(52, 71, 103)); 
            lbl.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, 8, 0),
                new MatteBorder(0, 0, 2, 0, new Color(105, 108, 255)) 
            ));
        } else {
            lbl.setForeground(new Color(160, 174, 192)); 
            lbl.setBorder(new EmptyBorder(0, 0, 10, 0));
        }

        lbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                currentMainTab = tabType;
                renderMainTabs();
                bodyLayout.show(bodyPanel, (tabType == MainTab.MOVEMENT) ? "MOVEMENT" : "COUNT");
            }
        });
        return lbl;
    }

    // =========================================================================
    //  SECTION 2: STOCK MOVEMENT UI
    // =========================================================================

    private void initMovementUI() {
        movementPanel = new JPanel(new BorderLayout(0, 30));
        movementPanel.setOpaque(false);
        movementPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        moveCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        moveCardsPanel.setOpaque(false);
        renderMovementCards();

        moveFormPanel = new Card(Color.WHITE);
        moveFormPanel.setLayout(new BorderLayout(20, 20)); 
        moveFormPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        renderMovementForm();

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setOpaque(false);
        topContainer.add(moveCardsPanel, BorderLayout.NORTH);
        
        movementPanel.add(topContainer, BorderLayout.NORTH);
        movementPanel.add(moveFormPanel, BorderLayout.CENTER);
    }

    private void renderMovementCards() {
        moveCardsPanel.removeAll();
        moveCardsPanel.add(createMoveCard("Stock In", "HQ to Outlet", "📥", MoveType.STOCK_IN));
        moveCardsPanel.add(createMoveCard("Stock Out", "Outlet to Outlet", "📤", MoveType.STOCK_OUT));
        moveCardsPanel.revalidate();
        moveCardsPanel.repaint();
    }

    private JPanel createMoveCard(String title, String subtitle, String icon, MoveType type) {
        boolean isActive = (currentMoveType == type);
        Color borderColor = isActive ? new Color(105, 108, 255) : new Color(230, 230, 230);
        
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(280, 90));
        card.setBorder(new CompoundBorder(
            new LineBorder(borderColor, isActive ? 2 : 1, true),
            new EmptyBorder(15, 20, 15, 20)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 28));
        card.add(iconLbl, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 15));
        t.setForeground(new Color(52, 71, 103));
        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("SansSerif", Font.PLAIN, 12));
        s.setForeground(Color.GRAY);
        textPanel.add(t);
        textPanel.add(s);
        card.add(textPanel, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(transferCartModel != null && transferCartModel.getRowCount() > 0) {
                    JOptionPane.showMessageDialog(null, "Please clear your transfer list before switching modes.");
                    return;
                }
                currentMoveType = type;
                renderMovementCards();
                updateMovementFormLogic();
            }
        });
        return card;
    }

    private void renderMovementForm() {
        moveFormPanel.removeAll();
        
        JPanel inputContainer = new JPanel(new GridBagLayout());
        inputContainer.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lbl = new JLabel("Transfer Details");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        lbl.setForeground(new Color(52, 71, 103));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        inputContainer.add(lbl, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        inputContainer.add(createLabel("Source (From)"), gbc);
        sourceCombo = new JComboBox<>();
        styleCombo(sourceCombo);
        gbc.gridx = 1;
        inputContainer.add(sourceCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputContainer.add(createLabel("Destination (To)"), gbc);
        destCombo = new JComboBox<>();
        styleCombo(destCombo);
        gbc.gridx = 1;
        inputContainer.add(destCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        inputContainer.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 4;
        inputContainer.add(createLabel("Model Code"), gbc);
        modelField = createStyledField();
        gbc.gridx = 1;
        inputContainer.add(modelField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        inputContainer.add(createLabel("Quantity"), gbc);
        qtyField = createStyledField();
        gbc.gridx = 1;
        inputContainer.add(qtyField, gbc);

        addToCartBtn = new JButton("+ Add to Transfer List");
        styleSecondaryBtn(addToCartBtn);
        addToCartBtn.setPreferredSize(new Dimension(250, 40));
        addToCartBtn.setBackground(new Color(240, 248, 255)); 
        addToCartBtn.setForeground(new Color(105, 108, 255));
        addToCartBtn.setBorder(new LineBorder(new Color(105, 108, 255)));
        addToCartBtn.addActionListener(e -> addItemToCart());
        
        gbc.gridx = 1; gbc.gridy = 6; gbc.insets = new Insets(20, 10, 10, 10);
        inputContainer.add(addToCartBtn, gbc);

        JPanel cartPanel = new JPanel(new BorderLayout(0, 10));
        cartPanel.setOpaque(false);
        cartPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
        
        JLabel cartTitle = new JLabel("Pending Transfers");
        cartTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        cartTitle.setForeground(Color.GRAY);
        cartPanel.add(cartTitle, BorderLayout.NORTH);

        String[] cols = {"Model", "Qty", "Action"};
        transferCartModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return col == 2; } 
        };
        transferCartTable = new JTable(transferCartModel);
        transferCartTable.setRowHeight(30);
        transferCartTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(transferCartTable);
        cartPanel.add(scroll, BorderLayout.CENTER);

        JButton removeBtn = new JButton("Remove Selected");
        styleSecondaryBtn(removeBtn);
        removeBtn.addActionListener(e -> {
            if (transferCartTable.isEditing()) transferCartTable.getCellEditor().stopCellEditing();
            int row = transferCartTable.getSelectedRow();
            if(row != -1) {
                transferCartModel.removeRow(row);
                checkCartState(); 
            }
        });
        
        executeBatchBtn = new JButton("Execute All Transfers");
        stylePrimaryBtn(executeBatchBtn);
        executeBatchBtn.addActionListener(e -> performBatchExecution());
        
        JPanel cartBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cartBottom.setOpaque(false);
        cartBottom.add(removeBtn);
        cartBottom.add(executeBatchBtn);
        cartPanel.add(cartBottom, BorderLayout.SOUTH);

        moveFormPanel.add(inputContainer, BorderLayout.WEST);
        moveFormPanel.add(cartPanel, BorderLayout.CENTER);

        updateMovementFormLogic();
    }

    private void updateMovementFormLogic() {
        sourceCombo.removeAllItems();
        destCombo.removeAllItems();

        if (currentMoveType == MoveType.STOCK_IN) {
            sourceCombo.addItem("HQ");
            sourceCombo.setEnabled(false);
            DataLoad.allOutlets.forEach(o -> destCombo.addItem(o.getOutletCode()));
        } else {
            DataLoad.allOutlets.forEach(o -> sourceCombo.addItem(o.getOutletCode()));
            DataLoad.allOutlets.forEach(o -> destCombo.addItem(o.getOutletCode()));
            sourceCombo.setEnabled(true);
        }
    }

    private void addItemToCart() {
        String from = (String) sourceCombo.getSelectedItem();
        String to = (String) destCombo.getSelectedItem();
        String modelCode = modelField.getText().trim();
        String qtyStr = qtyField.getText().trim();

        if (modelCode.isEmpty() || qtyStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill model and quantity."); return;
        }
        
        if (currentMoveType == MoveType.STOCK_OUT && from.equals(to)) {
            JOptionPane.showMessageDialog(this, "Source and Dest cannot be same."); return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) throw new NumberFormatException();

            Model target = DataLoad.allModels.stream()
                    .filter(m -> m.getModelCode().equalsIgnoreCase(modelCode))
                    .findFirst().orElse(null);

            if (target == null) {
                JOptionPane.showMessageDialog(this, "Model not found."); return;
            }

            if (currentMoveType == MoveType.STOCK_OUT) {
                int currSrc = target.getStock(from);
                int inCartQty = 0;
                for(int i=0; i<transferCartModel.getRowCount(); i++) {
                    if(transferCartModel.getValueAt(i, 0).equals(target.getModelCode())) {
                        inCartQty += (int)transferCartModel.getValueAt(i, 1);
                    }
                }
                
                if ((currSrc - inCartQty) < qty) {
                    JOptionPane.showMessageDialog(this, "Insufficient stock at source (" + from + ").\nAvailable: " + currSrc + "\nPending in Cart: " + inCartQty);
                    return;
                }
            }

            transferCartModel.addRow(new Object[]{target.getModelCode(), qty, "Remove"});
            
            modelField.setText("");
            qtyField.setText("");
            modelField.requestFocus();
            checkCartState();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Quantity.");
        }
    }

    private void checkCartState() {
        boolean hasItems = transferCartModel.getRowCount() > 0;
        sourceCombo.setEnabled(!hasItems && currentMoveType == MoveType.STOCK_OUT);
        destCombo.setEnabled(!hasItems);
    }

    // === BATCH EXECUTION WITH RECEIPT GENERATION ===
    private void performBatchExecution() {
        if (transferCartTable.isEditing()) transferCartTable.getCellEditor().stopCellEditing();

        if (transferCartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Transfer list is empty."); return;
        }

        String from = (String) sourceCombo.getSelectedItem();
        String to = (String) destCombo.getSelectedItem();
        
        // 1. Resolve Names for Receipt
        String fromName = getOutletName(from);
        String toName = getOutletName(to);
        String type = (currentMoveType == MoveType.STOCK_IN) ? "Stock In" : "Stock Out";
        String date = LocalDate.now().toString();
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String employee = (AuthService.getCurrentUser() != null) ? AuthService.getCurrentUser().getName() : "Unknown";

        // 2. Prepare Receipt Lines
        List<String> movementDetails = new ArrayList<>();
        int totalQty = 0;
        Set<Model> modelsToSave = new HashSet<>();

        // 3. Process Items
        for (int i = 0; i < transferCartModel.getRowCount(); i++) {
            String modelCode = (String) transferCartModel.getValueAt(i, 0);
            int qty = (int) transferCartModel.getValueAt(i, 1);

            Model target = DataLoad.allModels.stream()
                    .filter(m -> m.getModelCode().equals(modelCode))
                    .findFirst().orElse(null);

            if (target != null) {
                // Update Memory
                if (currentMoveType == MoveType.STOCK_IN) {
                    target.setStock(to, target.getStock(to) + qty);
                } else {
                    target.setStock(from, target.getStock(from) - qty);
                    target.setStock(to, target.getStock(to) + qty);
                }
                
                modelsToSave.add(target);
                movementDetails.add("- " + modelCode + ": " + qty);
                totalQty += qty;
            }
        }

        // 4. Update Database safely
        for (Model m : modelsToSave) {
            DatabaseHandler.saveModel(m); 
        }
        com.goldenhour.storage.CSVHandler.writeStock(DataLoad.allModels);

        // 5. Build Receipt String (CLI Format)
        String receipt = "=== " + type + " ===\n" +
                "Date: " + date + "\n" +
                "Time: " + time + "\n" +
                "From: " + from + " (" + fromName + ")\n" +
                "To: " + to + " (" + toName + ")\n" +
                "Models:\n" + String.join("\n", movementDetails) + "\n" +
                "Total Quantity: " + totalQty + "\n" +
                "Employee in Charge: " + employee;

        // 6. Save Receipt
        ReceiptHandler.appendReceipt(receipt);

        // 7. Show Receipt UI
        JTextArea receiptArea = new JTextArea(receipt);
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollReceipt = new JScrollPane(receiptArea);
        scrollReceipt.setPreferredSize(new Dimension(350, 400));

        JOptionPane.showMessageDialog(this, scrollReceipt, "Transaction Receipt", JOptionPane.PLAIN_MESSAGE);

        // 8. Clear UI
        transferCartModel.setRowCount(0);
        checkCartState();
    }

    private String getOutletName(String code) {
        if (code.equals("HQ")) return "HeadQuarters";
        return DataLoad.allOutlets.stream()
                .filter(o -> o.getOutletCode().equals(code))
                .map(Outlet::getOutletName)
                .findFirst()
                .orElse("Unknown");
    }

    // =========================================================================
    //  SECTION 3: STOCK COUNT UI
    // =========================================================================

    private void initCountUI() {
        countPanel = new JPanel(new BorderLayout(0, 20));
        countPanel.setOpaque(false);
        countPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        countProgressBar = new ProgressBarPanel();
        countPanel.add(countProgressBar, BorderLayout.NORTH);

        countCardLayout = new CardLayout();
        countContentPanel = new JPanel(countCardLayout);
        countContentPanel.setOpaque(false);

        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 50));
        selectionPanel.setOpaque(false);
        selectionPanel.add(createCountSelectionCard("Morning Count", "Daily opening check", "☀️", CountType.MORNING));
        selectionPanel.add(createCountSelectionCard("Night Count", "Closing inventory check", "🌙", CountType.NIGHT));
        countContentPanel.add(selectionPanel, "SELECTION");

        initCountInputPanel(); 

        reportPanel = new StockReportPanel(new ArrayList<>(), 0, 0);
        countContentPanel.add(new JScrollPane(reportPanel), "REPORT");

        countPanel.add(countContentPanel, BorderLayout.CENTER);
    }

    private JPanel createCountSelectionCard(String title, String subtitle, String icon, CountType type) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(320, 120));
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(230, 230, 230), 1, true),
            new EmptyBorder(25, 30, 25, 30)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 36));
        card.add(iconLbl, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 18));
        t.setForeground(new Color(52, 71, 103));
        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("SansSerif", Font.PLAIN, 13));
        s.setForeground(Color.GRAY);
        textPanel.add(t);
        textPanel.add(s);
        card.add(textPanel, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new CompoundBorder(new LineBorder(new Color(105, 108, 255), 2, true), new EmptyBorder(25, 30, 25, 30)));
            }
            public void mouseExited(MouseEvent e) {
                card.setBorder(new CompoundBorder(new LineBorder(new Color(230, 230, 230), 1, true), new EmptyBorder(25, 30, 25, 30)));
            }
            public void mouseClicked(MouseEvent e) {
                //currentCountType = type;
                currentCountStep = CountStep.INPUT;
                countProgressBar.repaint();
                loadCountInputData();
                countCardLayout.show(countContentPanel, "INPUT");
            }
        });
        return card;
    }

    private void initCountInputPanel() {
        JPanel inputPanel = new Card(Color.WHITE);
        inputPanel.setLayout(new BorderLayout(0, 20));
        inputPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        toolBar.setOpaque(false);
        JButton backBtn = new JButton("← Back");
        styleSecondaryBtn(backBtn);
        backBtn.addActionListener(e -> {
            currentCountStep = CountStep.SELECTION;
            countProgressBar.repaint();
            countCardLayout.show(countContentPanel, "SELECTION");
        });
        countOutletSelector = new JComboBox<>();
        countOutletSelector.setPreferredSize(new Dimension(150, 35));
        for(Outlet o : DataLoad.allOutlets) countOutletSelector.addItem(o.getOutletCode());
        countOutletSelector.addActionListener(e -> loadCountInputData());
        
        toolBar.add(backBtn);
        toolBar.add(new JLabel("Outlet:"));
        toolBar.add(countOutletSelector);

        String[] cols = {"Model Code", "System Record", "Physical Count (Input)"};
        countInputModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return col == 2; }
        };
        countInputTable = new JTable(countInputModel);
        countInputTable.setRowHeight(35);
        countInputTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JScrollPane scroll = new JScrollPane(countInputTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.setOpaque(false);
        JButton verifyBtn = new JButton("Generate Report >");
        stylePrimaryBtn(verifyBtn);
        verifyBtn.addActionListener(e -> generateCountReport());
        bottomBar.add(verifyBtn);

        inputPanel.add(toolBar, BorderLayout.NORTH);
        inputPanel.add(scroll, BorderLayout.CENTER);
        inputPanel.add(bottomBar, BorderLayout.SOUTH);
        
        countContentPanel.add(inputPanel, "INPUT");
    }

    private void loadCountInputData() {
        countInputModel.setRowCount(0);
        String outlet = (String) countOutletSelector.getSelectedItem();
        if(outlet == null) return;
        for (Model m : DataLoad.allModels) {
            countInputModel.addRow(new Object[]{ m.getModelCode(), m.getStock(outlet), "" });
        }
    }

    private void generateCountReport() {
        if (countInputTable.isCellEditable(countInputTable.getSelectedRow(), countInputTable.getSelectedColumn()))
            countInputTable.getCellEditor().stopCellEditing();

        List<CountResult> results = new ArrayList<>();
        int matches = 0;
        int mismatches = 0;

        for (int i = 0; i < countInputModel.getRowCount(); i++) {
            String model = (String) countInputModel.getValueAt(i, 0);
            int sys = (int) countInputModel.getValueAt(i, 1);
            String in = (String) countInputModel.getValueAt(i, 2);
            int phys = 0;
            try { phys = Integer.parseInt(in.trim()); } catch (Exception e) {}

            boolean isMatch = (sys == phys);
            if(isMatch) matches++; else mismatches++;
            results.add(new CountResult(model, sys, phys, isMatch));
        }

        countContentPanel.remove(2); 
        reportPanel = new StockReportPanel(results, matches, mismatches);
        JScrollPane scrollReport = new JScrollPane(reportPanel);
        scrollReport.setBorder(null);
        scrollReport.getVerticalScrollBar().setUnitIncrement(16);
        
        countContentPanel.add(scrollReport, "REPORT");
        currentCountStep = CountStep.REPORT;
        countProgressBar.repaint();
        countCardLayout.show(countContentPanel, "REPORT");
    }

    // =========================================================================
    //  CUSTOM COMPONENTS (PROGRESS BAR & CHART)
    // =========================================================================

    private class ProgressBarPanel extends JPanel {
        public ProgressBarPanel() { setPreferredSize(new Dimension(600, 80)); setOpaque(false); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int containerW = 500, containerH = 50;
            int startX = (w - containerW)/2, startY = (h - containerH)/2;

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(startX, startY, containerW, containerH, containerH, containerH);
            g2.setColor(new Color(220, 220, 220));
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(startX, startY, containerW, containerH, containerH, containerH);

            int gapForArrow = 40;
            int pillW = (containerW - gapForArrow)/2 - 6;
            int pillH = containerH - 8;
            int pillY = startY + 4;
            
            int centerX = startX + containerW/2;
            int leftPillX = centerX - gapForArrow/2 - pillW;
            int rightPillX = centerX + gapForArrow/2;

            boolean step1Active = (currentCountStep == CountStep.SELECTION || currentCountStep == CountStep.INPUT);

            g2.setColor(new Color(105, 108, 255));
            if (step1Active) g2.fillRoundRect(leftPillX, pillY, pillW, pillH, pillH, pillH);
            else g2.fillRoundRect(rightPillX, pillY, pillW, pillH, pillH, pillH);

            int arrowX = centerX - 10, arrowY = startY + containerH/2;
            g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(arrowX, arrowY, arrowX+20, arrowY);
            g2.drawLine(arrowX+15, arrowY-5, arrowX+20, arrowY);
            g2.drawLine(arrowX+15, arrowY+5, arrowX+20, arrowY);

            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            String t1 = "Morning | Night", t2 = "Report";
            
            g2.setColor(step1Active ? Color.WHITE : Color.GRAY);
            g2.drawString(t1, leftPillX + (pillW - fm.stringWidth(t1))/2, pillY + (pillH + fm.getAscent())/2 - 4);
            
            g2.setColor(!step1Active ? Color.WHITE : Color.GRAY);
            g2.drawString(t2, rightPillX + (pillW - fm.stringWidth(t2))/2, pillY + (pillH + fm.getAscent())/2 - 4);
        }
    }

    private class StockReportPanel extends JPanel {
        private List<CountResult> data;
        private int matches, mismatches;

        public StockReportPanel(List<CountResult> data, int matches, int mismatches) {
            this.data = data; this.matches = matches; this.mismatches = mismatches;
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(Math.max(800, data.size() * 100), 600));
            setLayout(null);
            
            JButton doneBtn = new JButton("Finish");
            stylePrimaryBtn(doneBtn);
            doneBtn.setBounds(20, 20, 100, 40);
            doneBtn.addActionListener(e -> {
                currentCountStep = CountStep.SELECTION;
                countProgressBar.repaint();
                countCardLayout.show(countContentPanel, "SELECTION");
            });
            add(doneBtn);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2.setColor(new Color(52, 71, 103));
            g2.drawString("Stock Count Report", 150, 50);

            int chartX = 50, chartY = 100, chartH = 300, barW = 40, gap = 60;
            int baseline = chartY + chartH;

            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(chartX, baseline, Math.max(getWidth()-50, data.size()*(barW+gap)+100), baseline);

            int maxQty = 1;
            for(CountResult r : data) maxQty = Math.max(maxQty, Math.max(r.sys, r.phys));

            int x = chartX + 20;
            int r = barW; 

            for (CountResult res : data) {
                int orangeQty = Math.min(res.sys, res.phys);
                int grayQty = Math.abs(res.sys - res.phys);

                int orangeH = (int) ((double) orangeQty / maxQty * (chartH - 20));
                int grayH = (int) ((double) grayQty / maxQty * (chartH - 20));

                if (orangeQty > 0 && orangeH < 2) orangeH = 2;
                if (grayQty > 0 && grayH < 2) grayH = 2;

                int orangeY = baseline - orangeH;
                g2.setColor(new Color(255, 171, 0));
                
                if (grayQty > 0) g2.fillRect(x, orangeY, barW, orangeH); 
                else drawRoundedTopBar(g2, x, orangeY, barW, orangeH, r); 

                if (grayQty > 0) {
                    g2.setColor(Color.GRAY);
                    drawRoundedTopBar(g2, x, orangeY - grayH, barW, grayH, r);
                }

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(res.model, x-10, baseline+25);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString("Cnt: "+res.phys, x, baseline+40);
                g2.drawString("Rec: "+res.sys, x, baseline+55);
                
                if(!res.match) { g2.setColor(new Color(234, 84, 85)); g2.drawString("Diff: "+(res.phys-res.sys), x, baseline+70); }
                else { g2.setColor(new Color(40, 199, 111)); g2.drawString("OK", x+10, baseline+70); }

                x += (barW + gap);
            }

            int footY = baseline + 100;
            g2.setColor(new Color(245, 247, 250));
            g2.fillRoundRect(50, footY, 600, 100, 20, 20);
            
            g2.setColor(new Color(52, 71, 103)); g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("Total Checked: "+data.size(), 80, footY+40);
            g2.setColor(new Color(40, 199, 111)); g2.drawString("✅ Tally Correct: "+matches, 250, footY+40);
            g2.setColor(mismatches>0 ? new Color(234,84,85) : Color.GRAY); g2.drawString("⚠️ Mismatches: "+mismatches, 450, footY+40);
        }

        private void drawRoundedTopBar(Graphics2D g2, int x, int y, int w, int h, int r) {
            g2.fillRoundRect(x, y, w, h, r, r);
            if (h > 0) g2.fillRect(x, y + Math.max(0, h-r/2), w, Math.min(h, r/2));
        }
    }

    private static class CountResult {
        String model; int sys, phys; boolean match;
        public CountResult(String m, int s, int p, boolean b) { model=m; sys=s; phys=p; match=b; }
    }

    // --- UTILS ---
    private JLabel createLabel(String t) { JLabel l=new JLabel(t); l.setFont(new Font("SansSerif",Font.BOLD,13)); l.setForeground(new Color(52,71,103)); return l; }
    private JTextField createStyledField() { JTextField f=new JTextField(20); f.setPreferredSize(new Dimension(250,40)); f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(217,222,227)), new EmptyBorder(5,10,5,10))); return f; }
    private void styleCombo(JComboBox b) { b.setBackground(Color.WHITE); b.setPreferredSize(new Dimension(250,40)); }
    private void stylePrimaryBtn(JButton b) { b.setBackground(new Color(105,108,255)); b.setForeground(Color.WHITE); b.setFocusPainted(false); b.setFont(new Font("SansSerif",Font.BOLD,14)); b.setPreferredSize(new Dimension(200,45)); }
    private void styleSecondaryBtn(JButton b) { b.setBackground(Color.WHITE); b.setForeground(Color.GRAY); b.setBorder(new LineBorder(Color.LIGHT_GRAY)); b.setFocusPainted(false); b.setFont(new Font("SansSerif",Font.BOLD,12)); b.setPreferredSize(new Dimension(100,35)); }
}