package com.goldenhour.gui.pos;

import com.goldenhour.categories.Employee;
import com.goldenhour.categories.Model;
import com.goldenhour.categories.Sales;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.gui.common.BackgroundPanel;
import com.goldenhour.gui.common.Card;
import com.goldenhour.service.loginregister.AuthService;
import com.goldenhour.storage.CSVHandler;
import com.goldenhour.storage.DatabaseHandler;
import com.goldenhour.storage.ReceiptHandler;
import com.goldenhour.utils.TimeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class POSPanel extends BackgroundPanel {

    // --- STATE MANAGEMENT ---
    private CardLayout cardLayout = new CardLayout();
    private JPanel contentPanel;
    private StepProgressBar progressBar;
    
    // Shared Data across steps
    private List<Sales> currentCart = new ArrayList<>();
    private double grandTotal = 0.0;
    private String currentOutlet = "C60";
    private String customerName = "Walk-in";
    private String paymentMethod = "Card";

    // Sub-Panels
    private CartStepPanel cartPanel;
    private PaymentStepPanel paymentPanel;
    private ConfirmationStepPanel confirmPanel;

    public POSPanel() {
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(20, 30, 30, 30));

        // 1. TOP PROGRESS BAR
        progressBar = new StepProgressBar();
        add(progressBar, BorderLayout.NORTH);

        // 2. MAIN CONTENT (CardLayout)
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        // Initialize Steps
        cartPanel = new CartStepPanel();
        paymentPanel = new PaymentStepPanel();
        confirmPanel = new ConfirmationStepPanel();

        contentPanel.add(cartPanel, "STEP_1");
        contentPanel.add(paymentPanel, "STEP_2");
        contentPanel.add(confirmPanel, "STEP_3");

        add(contentPanel, BorderLayout.CENTER);
    }

    // --- NAVIGATION LOGIC ---
    private void goToStep(int step) {
        progressBar.setCurrentStep(step);
        if (step == 1) cardLayout.show(contentPanel, "STEP_1");
        if (step == 2) {
            paymentPanel.updateSummary(); // Refresh total
            cardLayout.show(contentPanel, "STEP_2");
        }
        if (step == 3) {
            confirmPanel.loadReceipt();
            cardLayout.show(contentPanel, "STEP_3");
        }
    }

    // =================================================================================
    //  STEP 1: SHOPPING CART (Catalog + Cart)
    // =================================================================================
    class CartStepPanel extends JPanel {
        private JTable catalogTable, cartTable;
        private DefaultTableModel catalogModel, cartModel;
        private JLabel totalLabel;

        public CartStepPanel() {
            setLayout(new GridLayout(1, 2, 20, 0));
            setOpaque(false);

            // --- LEFT: CATALOG ---
            Card leftCard = new Card(Color.WHITE);
            leftCard.setLayout(new BorderLayout());
            leftCard.setBorder(new EmptyBorder(15, 15, 15, 15));
            
            // Filter Bar
            JPanel filterP = new JPanel(new BorderLayout()); filterP.setOpaque(false);
            JLabel title = new JLabel("Product Catalog");
            title.setFont(new Font("SansSerif", Font.BOLD, 16));
            title.setForeground(new Color(52, 71, 103));
            filterP.add(title, BorderLayout.WEST);
            
            JComboBox<String> outCombo = new JComboBox<>();
            DataLoad.allOutlets.forEach(o -> outCombo.addItem(o.getOutletCode()));
            outCombo.addActionListener(e -> {
                currentOutlet = (String) outCombo.getSelectedItem();
                refreshCatalog();
                currentCart.clear(); refreshCart();
            });
            filterP.add(outCombo, BorderLayout.EAST);
            leftCard.add(filterP, BorderLayout.NORTH);

            // Table
            String[] cols = {"Item", "Price", "Stock"};
            catalogModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) {return false;}};
            catalogTable = new JTable(catalogModel);
            styleTable(catalogTable);
            
            catalogTable.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) addToCart();
                }
            });
            
            // Allow Enter key to add item to cart
            catalogTable.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        e.consume();
                        addToCart();
                    }
                }
            });

            leftCard.add(new JScrollPane(catalogTable), BorderLayout.CENTER);
            leftCard.add(new JLabel("<html><center><font color='gray'>Double-click to add item</font></center></html>", SwingConstants.CENTER), BorderLayout.SOUTH);


            // --- RIGHT: CART ---
            Card rightCard = new Card(Color.WHITE);
            rightCard.setLayout(new BorderLayout());
            rightCard.setBorder(new EmptyBorder(15, 15, 15, 15));

            JLabel cartTitle = new JLabel("Customer's Bag");
            cartTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
            cartTitle.setForeground(new Color(52, 71, 103));
            rightCard.add(cartTitle, BorderLayout.NORTH);

            String[] cartCols = {"Item", "Qty", "Subtotal", ""};
            cartModel = new DefaultTableModel(cartCols, 0) { public boolean isCellEditable(int r, int c) {return false;}};
            cartTable = new JTable(cartModel);
            styleTable(cartTable);
            
            // Set up delete button column
            cartTable.getColumnModel().getColumn(3).setMaxWidth(50);
            cartTable.getColumnModel().getColumn(3).setMinWidth(50);
            cartTable.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel lbl = new JLabel("🗑", SwingConstants.CENTER);
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    lbl.setForeground(new Color(234, 84, 85));
                    lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    lbl.setOpaque(true);
                    lbl.setBackground(isSelected ? new Color(240, 240, 255) : Color.WHITE);
                    return lbl;
                }
            });
            
            // Handle delete click
            cartTable.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int row = cartTable.rowAtPoint(e.getPoint());
                    int col = cartTable.columnAtPoint(e.getPoint());
                    if (row >= 0 && col == 3) {
                        // Delete clicked
                        currentCart.remove(row);
                        refreshCart();
                    }
                    rightCard.requestFocusInWindow();
                }
            });
            
            JScrollPane cartScroll = new JScrollPane(cartTable);
            cartScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
            
            // Make the right card focusable for keyboard navigation
            rightCard.setFocusable(true);
            
            // Allow Enter key on right card to proceed to payment
            rightCard.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        e.consume();
                        if(currentCart.isEmpty()) {
                            JOptionPane.showMessageDialog(CartStepPanel.this, "Cart is empty! Please add items first.", "Empty Cart", JOptionPane.WARNING_MESSAGE);
                        } else {
                            goToStep(2);
                        }
                    }
                }
            });
            
            // Add focus listener to show frame around cart when focused
            rightCard.addFocusListener(new java.awt.event.FocusListener() {
                public void focusGained(java.awt.event.FocusEvent e) {
                    rightCard.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(105, 108, 255), 2),
                        new EmptyBorder(13, 13, 13, 13)
                    ));
                }
                public void focusLost(java.awt.event.FocusEvent e) {
                    rightCard.setBorder(new EmptyBorder(15, 15, 15, 15));
                }
            });
            
            // Allow click anywhere on right card to focus it
            rightCard.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    rightCard.requestFocusInWindow();
                }
            });
            cartScroll.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    rightCard.requestFocusInWindow();
                }
            });
            
            rightCard.add(cartScroll, BorderLayout.CENTER);

            // Bottom Total & Next
            JPanel bottomP = new JPanel(new BorderLayout(0, 10));
            bottomP.setOpaque(false);
            bottomP.setBorder(new EmptyBorder(15, 0, 0, 0));

            totalLabel = new JLabel("Total: RM 0.00");
            totalLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            
            JButton nextBtn = new JButton("Proceed to Payment \u2192");
            stylePrimaryBtn(nextBtn);
            nextBtn.addActionListener(e -> {
                if(currentCart.isEmpty()) JOptionPane.showMessageDialog(this, "Cart is empty!");
                else goToStep(2);
            });

            bottomP.add(totalLabel, BorderLayout.NORTH);
            bottomP.add(nextBtn, BorderLayout.SOUTH);
            rightCard.add(bottomP, BorderLayout.SOUTH);

            add(leftCard);
            add(rightCard);
            
            refreshCatalog();
        }

        private void refreshCatalog() {
            catalogModel.setRowCount(0);
            for (Model m : DataLoad.allModels) {
                catalogModel.addRow(new Object[]{m.getModelCode(), m.getPrice(), m.getStock(currentOutlet)});
            }
        }

        private void addToCart() {
            int row = catalogTable.getSelectedRow();
            if(row == -1) return;
            
            String code = (String) catalogModel.getValueAt(row, 0);
            double price = (double) catalogModel.getValueAt(row, 1);
            int stock = (int) catalogModel.getValueAt(row, 2);

            String qtyStr = JOptionPane.showInputDialog(this, "Enter Quantity:");
            if(qtyStr == null) return;
            try {
                int qty = Integer.parseInt(qtyStr);
                
                // Calculate total quantity already in cart for this item
                int existingQty = 0;
                Sales existingItem = null;
                for (Sales s : currentCart) {
                    if (s.getModel().equals(code)) {
                        existingQty = s.getQuantity();
                        existingItem = s;
                        break;
                    }
                }
                
                // Check if total quantity (existing + new) exceeds stock
                if(qty > 0 && (existingQty + qty) <= stock) {
                    if (existingItem != null) {
                        // Update existing item quantity and subtotal
                        existingItem.setQuantity(existingQty + qty);
                        existingItem.setSubtotal(existingItem.getQuantity() * price);
                    } else {
                        // Add new item to cart
                        Employee user = AuthService.getCurrentUser();
                        String empName = (user != null) ? user.getName() : "Unknown";
                        String empId = (user != null) ? user.getId() : "Unknown";

                        Sales s = new Sales(
                            TimeUtils.getDate(), 
                            TimeUtils.getTime(), 
                            "Walk-in", 
                            code, 
                            qty, 
                            price*qty, 
                            "PENDING", 
                            empName,
                            currentOutlet,
                            empId
                        );
                        currentCart.add(s);
                    }
                    refreshCart();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Quantity or Insufficient Stock");
                }
            } catch(Exception e){
                JOptionPane.showMessageDialog(this, "Invalid Number");
            }
        }

        private void refreshCart() {
            cartModel.setRowCount(0);
            grandTotal = 0;
            for(Sales s : currentCart) {
                cartModel.addRow(new Object[]{s.getModel(), s.getQuantity(), String.format("%.2f", s.getSubtotal()), "🗑"});
                grandTotal += s.getSubtotal();
            }
            totalLabel.setText("Total: RM " + String.format("%.2f", grandTotal));
        }
    }

    // =================================================================================
    //  STEP 2: PAYMENT DETAILS (Dynamic Form)
    // =================================================================================
    class PaymentStepPanel extends JPanel {
        // Shared
        private JTextField nameField;
        private JLabel summaryTotal;
        private JPanel summaryItemsPanel;
        
        // Method Buttons
        private JButton btnCard, btnCash, btnWallet;
        private String selectedMethod = "Card"; // Default

        // Card Fields
        private JPanel cardPanel;
        private JTextField cardField, cvvField, expField;

        // Cash Fields
        private JPanel cashPanel;
        private JTextField tenderField;
        private JLabel changeLabel;

        // E-Wallet Fields
        private JPanel walletPanel;
        private JTextField refField;

        // Container for switching forms
        private JPanel dynamicFormContainer;
        private CardLayout cardLayout;

        public PaymentStepPanel() {
            setLayout(new BorderLayout(20, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 100, 0, 100)); 

            // --- LEFT: MAIN FORM AREA ---
            Card formCard = new Card(Color.WHITE);
            formCard.setLayout(new BorderLayout(0, 20));
            formCard.setBorder(new EmptyBorder(30, 40, 30, 40));

            // 1. Header & Method Selector
            JPanel topSection = new JPanel(new GridLayout(2, 1, 0, 15));
            topSection.setOpaque(false);
            
            JLabel head = new JLabel("Payment Details");
            head.setFont(new Font("SansSerif", Font.BOLD, 22));
            head.setForeground(new Color(52, 71, 103));
            topSection.add(head);

            // Method Buttons (Card | Cash | Wallet)
            JPanel methodRow = new JPanel(new GridLayout(1, 3, 10, 0));
            methodRow.setOpaque(false);
            
            btnCard = createMethodBtn("Card", "💳", true);
            btnCash = createMethodBtn("Cash", "💵", false);
            btnWallet = createMethodBtn("E-Wallet", "📱", false);
            
            methodRow.add(btnCard);
            methodRow.add(btnCash);
            methodRow.add(btnWallet);
            topSection.add(methodRow);
            
            formCard.add(topSection, BorderLayout.NORTH);

            // 2. Customer Name (Always Visible)
            JPanel namePanel = new JPanel(new BorderLayout(0, 5));
            namePanel.setOpaque(false);
            namePanel.add(createLabel("Customer Name"), BorderLayout.NORTH);
            nameField = createField("Enter Name");
            namePanel.add(nameField, BorderLayout.CENTER);
            
            // 3. Dynamic Center Panel
            cardLayout = new CardLayout();
            dynamicFormContainer = new JPanel(cardLayout);
            dynamicFormContainer.setOpaque(false);

            // -- Create Sub-Panels --
            createCardPanel();
            createCashPanel();
            createWalletPanel();

            dynamicFormContainer.add(cardPanel, "Card");
            dynamicFormContainer.add(cashPanel, "Cash");
            dynamicFormContainer.add(walletPanel, "E-Wallet");

            // Combine Name + Dynamic Form
            JPanel centerContent = new JPanel(new BorderLayout(0, 15));
            centerContent.setOpaque(false);
            centerContent.add(namePanel, BorderLayout.NORTH);
            centerContent.add(dynamicFormContainer, BorderLayout.CENTER);
            
            formCard.add(centerContent, BorderLayout.CENTER);
            add(formCard, BorderLayout.CENTER);

            // --- RIGHT: SUMMARY SIDEBAR ---
            Card summaryCard = new Card(Color.WHITE);
            summaryCard.setPreferredSize(new Dimension(300, 0));
            summaryCard.setLayout(new BorderLayout());
            summaryCard.setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel sumContent = new JPanel(new BorderLayout(0, 15));
            sumContent.setOpaque(false);
            
            // Header
            JLabel summaryHeader = new JLabel("<html><b>Order Summary</b></html>");
            sumContent.add(summaryHeader, BorderLayout.NORTH);
            
            // Items List Panel (Scrollable)
            summaryItemsPanel = new JPanel();
            summaryItemsPanel.setLayout(new BoxLayout(summaryItemsPanel, BoxLayout.Y_AXIS));
            summaryItemsPanel.setOpaque(false);
            
            JScrollPane itemsScroll = new JScrollPane(summaryItemsPanel);
            itemsScroll.setOpaque(false);
            itemsScroll.getViewport().setOpaque(false);
            itemsScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
            itemsScroll.setPreferredSize(new Dimension(0, 150));
            
            sumContent.add(itemsScroll, BorderLayout.CENTER);
            
            // Total at bottom
            summaryTotal = new JLabel("Total: RM 0.00");
            summaryTotal.setFont(new Font("SansSerif", Font.BOLD, 18));
            summaryTotal.setForeground(new Color(105, 108, 255));
            summaryTotal.setHorizontalAlignment(SwingConstants.RIGHT);
            sumContent.add(summaryTotal, BorderLayout.SOUTH);
            
            summaryCard.add(sumContent, BorderLayout.CENTER);
            
            JButton payBtn = new JButton("Confirm Payment");
            stylePrimaryBtn(payBtn);
            payBtn.addActionListener(e -> processPayment());
            
            JButton backBtn = new JButton("Back");
            backBtn.setBackground(Color.WHITE);
            backBtn.addActionListener(e -> goToStep(1));

            JPanel btnP = new JPanel(new GridLayout(2, 1, 0, 10));
            btnP.setOpaque(false);
            btnP.add(payBtn); btnP.add(backBtn);
            summaryCard.add(btnP, BorderLayout.SOUTH);

            add(summaryCard, BorderLayout.EAST);
        }

        // --- SUB-PANEL CREATION METHODS ---

        private void createCardPanel() {
            cardPanel = new JPanel(new GridBagLayout());
            cardPanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(0, 0, 15, 0); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = 0;

            cardPanel.add(createLabel("Card Number"), gbc);
            cardField = createField("1234 5678 1234 5678");
            gbc.gridy++; cardPanel.add(cardField, gbc);

            JPanel split = new JPanel(new GridLayout(1, 2, 20, 0));
            split.setOpaque(false);
            JPanel p1 = new JPanel(new BorderLayout()); p1.setOpaque(false);
            p1.add(createLabel("Expiry Date"), BorderLayout.NORTH);
            expField = createField("MM/YY");
            p1.add(expField, BorderLayout.CENTER);
            JPanel p2 = new JPanel(new BorderLayout()); p2.setOpaque(false);
            p2.add(createLabel("CVV"), BorderLayout.NORTH);
            cvvField = createField("123");
            p2.add(cvvField, BorderLayout.CENTER);
            split.add(p1); split.add(p2);
            
            gbc.gridy++; cardPanel.add(split, gbc);
            
            // Push content to top
            gbc.weighty = 1.0; gbc.gridy++; cardPanel.add(new JLabel(), gbc);
        }

        private void createCashPanel() {
            cashPanel = new JPanel(new GridBagLayout());
            cashPanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(0, 0, 15, 0); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = 0;

            cashPanel.add(createLabel("Amount Tendered (RM)"), gbc);
            
            tenderField = createField("0.00");
            // Auto-Calculate Change Logic
            tenderField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { calcChange(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { calcChange(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { calcChange(); }
            });
            
            gbc.gridy++; cashPanel.add(tenderField, gbc);

            changeLabel = new JLabel("Change: RM 0.00");
            changeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            changeLabel.setForeground(new Color(40, 199, 111)); // Green
            gbc.gridy++; cashPanel.add(changeLabel, gbc);
            
            gbc.weighty = 1.0; gbc.gridy++; cashPanel.add(new JLabel(), gbc);
        }

        private void createWalletPanel() {
            walletPanel = new JPanel(new GridBagLayout());
            walletPanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(0, 0, 15, 0); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = 0;

            walletPanel.add(createLabel("Transaction Reference ID"), gbc);
            refField = createField("e.g. TNG-123456789");
            gbc.gridy++; walletPanel.add(refField, gbc);
            
            // Add QR Placeholder Text
            JLabel note = new JLabel("<html><i>Please scan the DuitNow QR code at the counter.</i></html>");
            note.setForeground(Color.GRAY);
            gbc.gridy++; walletPanel.add(note, gbc);

            gbc.weighty = 1.0; gbc.gridy++; walletPanel.add(new JLabel(), gbc);
        }

        // --- LOGIC ---
        
        private void switchMethod(String method) {
            this.selectedMethod = method;
            
            // Visual Update
            styleMethodBtn(btnCard, method.equals("Card"));
            styleMethodBtn(btnCash, method.equals("Cash"));
            styleMethodBtn(btnWallet, method.equals("E-Wallet"));
            
            // Layout Update
            cardLayout.show(dynamicFormContainer, method);
        }

        private void calcChange() {
            try {
                double tendered = Double.parseDouble(tenderField.getText());
                double change = tendered - grandTotal;
                if(change < 0) changeLabel.setText("Change: RM 0.00 (Insufficient)");
                else changeLabel.setText("Change: RM " + String.format("%.2f", change));
            } catch (Exception e) {
                changeLabel.setText("Change: RM 0.00");
            }
        }

        public void updateSummary() {
            // Update total
            summaryTotal.setText("Total: RM " + String.format("%.2f", grandTotal));
            
            // Update items list
            summaryItemsPanel.removeAll();
            for (Sales item : currentCart) {
                JPanel itemRow = new JPanel(new BorderLayout());
                itemRow.setOpaque(false);
                itemRow.setBorder(new EmptyBorder(8, 5, 8, 5));
                
                // Left: Model Name
                JLabel modelLabel = new JLabel(item.getModel());
                modelLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
                modelLabel.setForeground(new Color(52, 71, 103));
                
                // Right: Quantity
                JLabel qtyLabel = new JLabel("x " + item.getQuantity());
                qtyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
                qtyLabel.setForeground(new Color(105, 108, 255));
                qtyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                
                itemRow.add(modelLabel, BorderLayout.WEST);
                itemRow.add(qtyLabel, BorderLayout.EAST);
                
                summaryItemsPanel.add(itemRow);
                
                // Add separator
                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(240, 240, 245));
                summaryItemsPanel.add(Box.createVerticalStrut(3));
                summaryItemsPanel.add(sep);
                summaryItemsPanel.add(Box.createVerticalStrut(3));
            }
            
            // Push items to the top by adding glue at the bottom
            summaryItemsPanel.add(Box.createVerticalGlue());
            
            summaryItemsPanel.revalidate();
            summaryItemsPanel.repaint();
        }

        private void processPayment() {
            if(nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter Customer Name"); return;
            }

            // Validation per method
            if(selectedMethod.equals("Cash")) {
                try {
                    double tender = Double.parseDouble(tenderField.getText());
                    if(tender < grandTotal) {
                        JOptionPane.showMessageDialog(this, "Insufficient Cash Amount!"); return;
                    }
                } catch(Exception e) {
                    JOptionPane.showMessageDialog(this, "Invalid Cash Amount"); return;
                }
            } else if (selectedMethod.equals("E-Wallet")) {
                if(refField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter Transaction Reference ID"); return;
                }
            }

            // Save Data
            customerName = nameField.getText();
            paymentMethod = selectedMethod; // Set the method for DB saving

            // --- FINAL TRANSACTION LOGIC ---
            for (Sales item : currentCart) {
                item.setCustomerName(customerName);
                item.setTransactionMethod(paymentMethod); 
                
                // Ensure Outlet and Employee are set (Inherit from session)
                item.setOutletCode(currentOutlet);
                if(AuthService.getCurrentUser() != null) {
                    item.setEmployee(AuthService.getCurrentUser().getName());
                    item.setEmployeeId(AuthService.getCurrentUser().getId());
                }

                // Update Stock
                Model m = DataLoad.allModels.stream().filter(mod -> mod.getModelCode().equals(item.getModel())).findFirst().orElse(null);
                if (m != null) {
                    int newStock = m.getStock(currentOutlet) - item.getQuantity();
                    m.setStock(currentOutlet, newStock);
                    DatabaseHandler.updateStock(m.getModelCode(), currentOutlet, newStock);
                }
                // Save Sale
                DataLoad.allSales.add(item);
                DatabaseHandler.saveSale(item);
                CSVHandler.appendSale(item); 
            }
            CSVHandler.writeStock(DataLoad.allModels);
            
            goToStep(3); // Success
        }

        // --- UI HELPERS ---

        private JButton createMethodBtn(String text, String icon, boolean active) {
            JButton b = new JButton(icon + " " + text);
            b.setFont(new Font("SansSerif", Font.BOLD, 12));
            b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            styleMethodBtn(b, active);
            b.addActionListener(e -> switchMethod(text));
            return b;
        }

        private void styleMethodBtn(JButton b, boolean active) {
            if(active) {
                b.setBackground(new Color(105, 108, 255));
                b.setForeground(Color.WHITE);
                b.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            } else {
                b.setBackground(new Color(240, 240, 245));
                b.setForeground(Color.GRAY);
                b.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            }
        }

        private JLabel createLabel(String t) {
            JLabel l = new JLabel(t);
            l.setForeground(Color.GRAY);
            l.setFont(new Font("SansSerif", Font.PLAIN, 12));
            return l;
        }

        private JTextField createField(String placeholder) {
            JTextField f = new JTextField();
            f.setPreferredSize(new Dimension(0, 40));
            f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220)),
                BorderFactory.createEmptyBorder(5,10,5,10)
            ));
            return f;
        }
    }

    // =================================================================================
    //  STEP 3: CONFIRMATION (Sneat Invoice Style)
    // =================================================================================
    class ConfirmationStepPanel extends JPanel {
        private JPanel invoiceContent; // The "Paper" part
        private String receiptForStorage = "";

        public ConfirmationStepPanel() {
            setLayout(new BorderLayout()); 
            setOpaque(false);
            
            // --- 1. THE INVOICE AREA (CENTER) ---
            Card invoiceCard = new Card(Color.WHITE);
            invoiceCard.setLayout(new BorderLayout());
            invoiceCard.setBorder(new EmptyBorder(40, 50, 40, 50)); 

            invoiceContent = new JPanel();
            invoiceContent.setLayout(new BoxLayout(invoiceContent, BoxLayout.Y_AXIS));
            invoiceContent.setOpaque(false);
            
            invoiceCard.add(invoiceContent, BorderLayout.CENTER);
            
            // Scroll pane for the invoice only
            JScrollPane scroll = new JScrollPane(invoiceCard);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            
            add(scroll, BorderLayout.CENTER);

            // --- 2. THE BUTTONS AREA (BOTTOM - FIXED) ---
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
            btnPanel.setOpaque(false);
            btnPanel.setBackground(new Color(245, 247, 250)); 
            
            JButton newOrderBtn = new JButton("Start New Order");
            stylePrimaryBtn(newOrderBtn);
            newOrderBtn.addActionListener(e -> resetPOS());
            
            JButton printBtn = new JButton("Print Invoice");
            printBtn.setBackground(Color.WHITE);
            printBtn.setForeground(new Color(52, 71, 103));
            printBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
            printBtn.setPreferredSize(new Dimension(150, 45));
            printBtn.addActionListener(e -> saveReceiptToFile());

            btnPanel.add(printBtn);
            btnPanel.add(newOrderBtn);
            
            add(btnPanel, BorderLayout.SOUTH);
        }

        public void loadReceipt() {
            buildFancyInvoiceUI();
            generateStoredReceipt();
        }

        private void buildFancyInvoiceUI() {
            invoiceContent.removeAll();
            
            // --- HEADER ---
            JPanel header = new JPanel(new BorderLayout()); 
            header.setOpaque(false);
            
            // Left: Logo + Address
            JPanel leftHead = new JPanel(); 
            leftHead.setLayout(new BoxLayout(leftHead, BoxLayout.Y_AXIS));
            leftHead.setOpaque(false);

            JLabel logo = new JLabel();
            try {
                // Adjust path as needed for your project structure
                ImageIcon icon = new ImageIcon(getClass().getResource("/resources/images/logo.jpeg")); 
                Image img = icon.getImage().getScaledInstance(180, 60, Image.SCALE_SMOOTH); // Resize logo
                logo.setIcon(new ImageIcon(img));
            } catch (Exception e) {
                // Fallback if image missing
                logo.setText("<html><h1 style='color:#344767; font-family:sans-serif;'>GoldenHour</h1></html>");
            }
            logo.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftHead.add(logo);
            leftHead.add(Box.createVerticalStrut(10));
            
            // Address
            String addrFont = "font-family:sans-serif; font-size:11px; color:#697a8d;";
            JLabel address = new JLabel("<html><div style='" + addrFont + "'>" +
                    "15, T1, Blok Mulu, Kolej Kediaman Tun Ahmad Zaidi<br>" +
                    "50603, Kuala Lumpur, W.P. Kuala Lumpur</div></html>");
            address.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftHead.add(address);
            
            // Right: Invoice Details
            JPanel rightHead = new JPanel();
            rightHead.setLayout(new BoxLayout(rightHead, BoxLayout.Y_AXIS));
            rightHead.setOpaque(false);
            
            JLabel invNum = new JLabel("Invoice #696969");
            invNum.setFont(new Font("SansSerif", Font.BOLD, 20));
            invNum.setForeground(new Color(52, 71, 103));
            invNum.setAlignmentX(Component.RIGHT_ALIGNMENT);
            
            JLabel dateIssue = new JLabel("Date Issued: " + TimeUtils.getDate());
            dateIssue.setForeground(Color.GRAY);
            dateIssue.setFont(new Font("SansSerif", Font.PLAIN, 12));
            dateIssue.setAlignmentX(Component.RIGHT_ALIGNMENT);

            rightHead.add(invNum);
            rightHead.add(Box.createVerticalStrut(5));
            rightHead.add(dateIssue);
            
            header.add(leftHead, BorderLayout.WEST);
            header.add(rightHead, BorderLayout.EAST);
            invoiceContent.add(header);
            
            invoiceContent.add(Box.createVerticalStrut(40));
            
            // --- INVOICE TO & ISSUED BY ---
            JPanel flowWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            flowWrapper.setOpaque(false);

            JPanel leftInfoBox = new JPanel();
            leftInfoBox.setLayout(new BoxLayout(leftInfoBox, BoxLayout.Y_AXIS));
            leftInfoBox.setOpaque(false);
            
            // Invoice To
            JLabel toLbl = new JLabel("Invoice To:");
            toLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            toLbl.setForeground(new Color(52, 71, 103));
            toLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JLabel custName = new JLabel(customerName);
            custName.setFont(new Font("SansSerif", Font.PLAIN, 14));
            custName.setForeground(new Color(105, 122, 141));
            custName.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            leftInfoBox.add(toLbl);
            leftInfoBox.add(Box.createVerticalStrut(2));
            leftInfoBox.add(custName);
            
            leftInfoBox.add(Box.createVerticalStrut(20));
            
            // Issued By
            String emp = (AuthService.getCurrentUser() != null) ? AuthService.getCurrentUser().getName() : "Unknown";
            JLabel byLbl = new JLabel("Issued By:");
            byLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            byLbl.setForeground(new Color(52, 71, 103));
            byLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JLabel empName = new JLabel(emp);
            empName.setFont(new Font("SansSerif", Font.PLAIN, 14));
            empName.setForeground(new Color(105, 122, 141));
            empName.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            leftInfoBox.add(byLbl);
            leftInfoBox.add(Box.createVerticalStrut(2));
            leftInfoBox.add(empName);

            flowWrapper.add(leftInfoBox);
            invoiceContent.add(flowWrapper);
            
            invoiceContent.add(Box.createVerticalStrut(30));

            // --- TABLE ---
            JPanel tablePanel = new JPanel(new GridBagLayout());
            tablePanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            
            // Headers
            String[] headers = {"ITEM", "DESC", "COST", "QTY", "PRICE"};
            gbc.gridy = 0; 
            for(int i=0; i<headers.length; i++) {
                gbc.gridx = i;
                if(i == 0) gbc.weightx = 0.3; // Item col wider
                else if(i == 1) gbc.weightx = 0.3;
                else gbc.weightx = 0.1;
                
                JLabel h = new JLabel(headers[i]);
                h.setFont(new Font("SansSerif", Font.BOLD, 11));
                h.setForeground(new Color(52, 71, 103));
                h.setBorder(new EmptyBorder(10, 5, 10, 5));
                h.setOpaque(true);
                h.setBackground(new Color(249, 250, 251)); // Light grey header
                tablePanel.add(h, gbc);
            }

            // Data Rows
            int rowIdx = 1;
            for(Sales s : currentCart) {
                gbc.gridy = rowIdx++;
                addCell(tablePanel, s.getModel(), gbc, 0, false);
                addCell(tablePanel, "Luxury Watch", gbc, 1, false);
                addCell(tablePanel, String.format("RM %.2f", s.getSubtotal()/s.getQuantity()), gbc, 2, false);
                addCell(tablePanel, String.valueOf(s.getQuantity()), gbc, 3, false);
                addCell(tablePanel, String.format("RM %.2f", s.getSubtotal()), gbc, 4, false);
                
                gbc.gridy = rowIdx++;
                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(236, 238, 241));
                gbc.gridx = 0; gbc.gridwidth = 5;
                tablePanel.add(sep, gbc);
                gbc.gridwidth = 1; 
            }
            
            invoiceContent.add(tablePanel);
            invoiceContent.add(Box.createVerticalStrut(30));

            // --- FOOTER ---
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            footer.setOpaque(false);
            
            JPanel totals = new JPanel(new GridLayout(2, 2, 30, 5));
            totals.setOpaque(false);
            
            totals.add(new JLabel("Subtotal:"));
            JLabel subVal = new JLabel(String.format("RM %.2f", grandTotal));
            subVal.setHorizontalAlignment(SwingConstants.RIGHT);
            totals.add(subVal);
            
            JLabel totLbl = new JLabel("Total:");
            totLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            totals.add(totLbl);
            
            JLabel totVal = new JLabel(String.format("RM %.2f", grandTotal));
            totVal.setFont(new Font("SansSerif", Font.BOLD, 14));
            totVal.setHorizontalAlignment(SwingConstants.RIGHT);
            totals.add(totVal);
            
            footer.add(totals);
            invoiceContent.add(footer);

            invoiceContent.revalidate();
            invoiceContent.repaint();
        }

        private void addCell(JPanel p, String text, GridBagConstraints gbc, int x, boolean bold) {
            gbc.gridx = x;
            JLabel l = new JLabel(text);
            l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 12));
            l.setForeground(new Color(105, 122, 141));
            l.setBorder(new EmptyBorder(12, 5, 12, 5));
            p.add(l, gbc);
        }

        private void generateStoredReceipt() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Sales Receipt ===\n");
            sb.append("Date: " + TimeUtils.getDate() + "\n");
            sb.append("Time: " + TimeUtils.getTime() + "\n");
            sb.append("Outlet: " + currentOutlet + "\n");
            sb.append("Customer: " + customerName + "\n");
            sb.append("----------------------------------\n");
            sb.append("ITEMS PURCHASED:\n");
            for(Sales s : currentCart) {
                sb.append(String.format("%-18s x %-3d RM %8.2f\n", 
                    s.getModel().length() > 18 ? s.getModel().substring(0,15)+"..." : s.getModel(), 
                    s.getQuantity(), s.getSubtotal()));
            }
            sb.append("----------------------------------\n");
            sb.append("Total Items: " + currentCart.size() + "\n");
            sb.append("Grand Total: RM " + String.format("%.2f", grandTotal) + "\n");
            sb.append("Method: " + paymentMethod + "\n");
            String emp = (AuthService.getCurrentUser() != null) ? AuthService.getCurrentUser().getName() : "Unknown";
            sb.append("Employee: " + emp + "\n");
            receiptForStorage = sb.toString();
        }

        private void saveReceiptToFile() {
            String fileName = ReceiptHandler.appendSalesReceipt(receiptForStorage, "sales");
            JOptionPane.showMessageDialog(this, "Receipt saved to " + fileName);
        }
    }

    private void resetPOS() {
        currentCart.clear();
        customerName = "Walk-in";
        grandTotal = 0;
        cartPanel.refreshCart();
        cartPanel.refreshCatalog();
        paymentPanel.nameField.setText("");
        goToStep(1);
    }

    // =================================================================================
    //  UI COMPONENT: PROGRESS BAR (Icons + Lines)
    // =================================================================================
    class StepProgressBar extends JPanel {
        private int currentStep = 1;
        
        public StepProgressBar() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 80));
        }
        
        public void setCurrentStep(int s) {
            this.currentStep = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int steps = 3;
            int stepW = w / steps;

            // Draw Connecting Line
            g2.setColor(new Color(220, 220, 220));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(stepW/2, h/2 - 10, w - stepW/2, h/2 - 10);

            // Draw Active Line
            g2.setColor(new Color(111, 66, 193)); // Purple
            if(currentStep > 1) g2.drawLine(stepW/2, h/2 - 10, stepW/2 + stepW * (currentStep-1), h/2 - 10);

            String[] labels = {"Cart", "Payment", "Confirm"};
            String[] icons = {"\uD83D\uDED2", "\uD83D\uDCB3", "\u2705"}; // Cart, Card, Check

            for(int i=0; i<steps; i++) {
                int cx = stepW/2 + (i * stepW);
                int cy = h/2 - 10;
                boolean active = (i + 1) <= currentStep;

                // Circle Bg
                g2.setColor(active ? new Color(111, 66, 193) : Color.WHITE);
                g2.fillOval(cx - 20, cy - 20, 40, 40);
                g2.setColor(active ? new Color(111, 66, 193) : new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(cx - 20, cy - 20, 40, 40);

                // Icon
                g2.setColor(active ? Color.WHITE : Color.GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(icons[i], cx - fm.stringWidth(icons[i])/2, cy + 6);

                // Text Label
                g2.setColor(active ? new Color(52, 71, 103) : Color.GRAY);
                g2.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 12));
                String lbl = labels[i];
                g2.drawString(lbl, cx - g2.getFontMetrics().stringWidth(lbl)/2, cy + 35);
            }
        }
    }

    // Helper Styles
    private void styleTable(JTable t) {
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.getTableHeader().setBackground(Color.WHITE);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
    }
    private void stylePrimaryBtn(JButton b) {
        b.setBackground(new Color(111, 66, 193));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(200, 45));
    }
}