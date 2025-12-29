package com.goldenhour.gui.admin;

import com.goldenhour.categories.*;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.gui.common.BackgroundPanel;
import com.goldenhour.gui.common.Card;
import com.goldenhour.storage.CSVHandler;
import com.goldenhour.storage.DatabaseHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class DatabaseViewerPanel extends BackgroundPanel {

    private JComboBox<String> tableSelector;
    private JComboBox<String> outletFilter;
    private JTextField searchField;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private String currentCategory = "Employees";

    public DatabaseViewerPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 30, 30, 30));

        // === 1. TOP CONTROL PANEL ===
        JPanel topContainer = new JPanel(new GridLayout(2, 1, 0, 10));
        topContainer.setOpaque(false);
        topContainer.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Row 1: Selectors & Actions
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        row1.setOpaque(false);

        JLabel lbl = new JLabel("Data Source:");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(new Color(52, 71, 103));

        String[] tables = {"Employees", "Stock Inventory", "Attendance", "Sales Records", "Outlets List"};
        tableSelector = new JComboBox<>(tables);
        tableSelector.setPreferredSize(new Dimension(180, 35));
        tableSelector.addActionListener(e -> switchTable((String) tableSelector.getSelectedItem()));

        JButton addBtn = new JButton("➕ Add New");
        styleActionBtn(addBtn, new Color(40, 199, 111)); // Green
        addBtn.addActionListener(e -> performAddAction());

        JButton saveBtn = new JButton("💾 Save Changes");
        styleActionBtn(saveBtn, new Color(105, 108, 255)); // Purple
        saveBtn.addActionListener(e -> saveCurrentData());

        JButton deleteBtn = new JButton("🗑 Delete Row");
        styleActionBtn(deleteBtn, new Color(234, 84, 85)); // Red
        deleteBtn.addActionListener(e -> deleteSelectedRow());

        row1.add(lbl);
        row1.add(tableSelector);
        row1.add(addBtn);
        row1.add(saveBtn);
        row1.add(deleteBtn);

        // Row 2: Filters
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        row2.setOpaque(false);

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        searchLbl.setForeground(new Color(52, 71, 103));

        searchField = new JTextField(15);
        searchField.setPreferredSize(new Dimension(150, 35));
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filterData(); }
        });

        JLabel outletLbl = new JLabel("Filter Outlet:");
        outletLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        outletLbl.setForeground(new Color(52, 71, 103));

        outletFilter = new JComboBox<>();
        outletFilter.setPreferredSize(new Dimension(120, 35));
        outletFilter.addActionListener(e -> loadTableData());

        row2.add(searchLbl);
        row2.add(searchField);
        row2.add(outletLbl);
        row2.add(outletFilter);

        topContainer.add(row1);
        topContainer.add(row2);
        add(topContainer, BorderLayout.NORTH);

        // === 2. TABLE AREA ===
        Card card = new Card(Color.WHITE);
        card.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                // LOCK Logic
                if (currentCategory.equals("Stock Inventory")) {
                    String selectedOutlet = (String) outletFilter.getSelectedItem();
                    // Lock 'Total Stock' column if "All Outlets" is selected
                    if (column == 2 && "All Outlets".equals(selectedOutlet)) return false; 
                    if (column == 0) return false; // Lock Model Code
                }
                if (currentCategory.equals("Employees") && column == 0) return false; // Lock ID
                if (currentCategory.equals("Outlets List") && column == 0) return false; // Lock ID
                return true; 
            }
        };

        dataTable = new JTable(tableModel);
        dataTable.setRowHeight(30);
        dataTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dataTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        dataTable.getTableHeader().setBackground(new Color(240, 242, 245));
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(dataTable);
        scroll.setBorder(null);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        // Initial Load
        refreshOutletCombo();
        switchTable("Employees");
    }

    // --- LOGIC METHODS ---

    private void refreshOutletCombo() {
        outletFilter.removeAllItems();
        outletFilter.addItem("All Outlets");
        for(Outlet o : DataLoad.allOutlets) {
            outletFilter.addItem(o.getOutletCode());
        }
    }

    private void switchTable(String category) {
        currentCategory = category;
        // FIXED: Only enable outlet filter for Stock (Sales doesn't support it yet)
        outletFilter.setEnabled(category.equals("Stock Inventory"));
        searchField.setText(""); 
        loadTableData();
    }

    private void loadTableData() {
        tableModel.setRowCount(0);
        String selectedOutlet = (String) outletFilter.getSelectedItem();
        if(selectedOutlet == null) selectedOutlet = "All Outlets";

        // === CRITICAL FIX: FETCH FRESH DATA FROM DB ===
        // This ensures the Admin Panel sees exactly what is in the SQLite Database
        
        try {
            switch (currentCategory) {
                case "Employees":
                    // 1. Fetch Fresh
                    DataLoad.allEmployees = DatabaseHandler.fetchAllEmployees(); 
                    
                    tableModel.setColumnIdentifiers(new String[]{"ID", "Name", "Role", "Password"});
                    for (Employee e : DataLoad.allEmployees) {
                        tableModel.addRow(new Object[]{e.getId(), e.getName(), e.getRole(), e.getPassword()});
                    }
                    break;

                case "Stock Inventory":
                    // 1. Fetch Fresh
                    DataLoad.allModels = DatabaseHandler.fetchAllModelsWithStock();
                    
                    boolean isSpecific = !"All Outlets".equals(selectedOutlet);
                    String stockCol = isSpecific ? "Stock (" + selectedOutlet + ")" : "Total Stock (Read-Only)";
                    tableModel.setColumnIdentifiers(new String[]{"Model Code", "Price (RM)", stockCol});

                    for (Model m : DataLoad.allModels) {
                        int qty = isSpecific ? m.getStock(selectedOutlet) : 
                                  m.getStockPerOutlet().values().stream().mapToInt(Integer::intValue).sum();
                        tableModel.addRow(new Object[]{m.getModelCode(), m.getPrice(), qty});
                    }
                    break;

                case "Attendance":
                    // 1. Fetch Fresh
                    DataLoad.allAttendance = DatabaseHandler.fetchAllAttendance();
                    
                    tableModel.setColumnIdentifiers(new String[]{"Emp ID", "Name", "Date", "Clock In", "Clock Out"});
                    for (Attendance a : DataLoad.allAttendance) {
                        tableModel.addRow(new Object[]{
                            a.getEmployeeId(), 
                            a.getEmployeeName(), // Changed from getName() to getEmployeeName() if needed
                            a.getDate(), 
                            a.getClockInTime(), 
                            a.getClockOutTime()
                        });
                    }
                    break;

                case "Sales Records":
                    // 1. Fetch Fresh
                    DataLoad.allSales = DatabaseHandler.fetchAllSales();
                    
                    tableModel.setColumnIdentifiers(new String[]{"Date", "Time","Outlet", "Customer", "Model", "Total (RM)", "Method"});
                    for (Sales s : DataLoad.allSales) {
                        tableModel.addRow(new Object[]{
                            s.getDate(), 
                            s.getTime(), 
                            s.getOutletCode(),
                            s.getCustomerName(), 
                            s.getModel(), 
                            s.getSubtotal(), 
                            s.getTransactionMethod()
                        });
                    }
                    break;
                    
                case "Outlets List":
                     // 1. Fetch Fresh
                     DataLoad.allOutlets = DatabaseHandler.fetchAllOutlets();
                     
                     tableModel.setColumnIdentifiers(new String[]{"Outlet ID", "Name / Location"});
                     for(Outlet o : DataLoad.allOutlets) {
                         tableModel.addRow(new Object[]{o.getOutletCode(), o.getOutletName()});
                     }
                     break;
            }
        } catch (Exception e) {
            System.err.println("Error loading table data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- ADD BUTTON LOGIC ---
    private void performAddAction() {
        switch (currentCategory) {
            case "Stock Inventory":
                String code = JOptionPane.showInputDialog("Enter New Model Code:");
                if(code == null || code.trim().isEmpty()) return;
                String priceStr = JOptionPane.showInputDialog("Enter Price (RM):");
                try {
                    double price = Double.parseDouble(priceStr);
                    Model newM = new Model(code, price);
                    // Initialize stock for all outlets to 0
                    for(Outlet o : DataLoad.allOutlets) newM.setStock(o.getOutletCode(), 0);
                    DataLoad.allModels.add(newM);
                    CSVHandler.writeStock(DataLoad.allModels);
                    loadTableData();
                    JOptionPane.showMessageDialog(this, "New Model Added!");
                } catch(Exception e) { JOptionPane.showMessageDialog(this, "Invalid Price"); }
                break;

            case "Outlets List":
                String oid = JOptionPane.showInputDialog("Enter New Outlet ID (e.g., C99):");
                if(oid == null || oid.trim().isEmpty()) return;
                String name = JOptionPane.showInputDialog("Enter Location Name:");
                
                DataLoad.allOutlets.add(new Outlet(oid, name));
                // Add this new outlet to every existing stock model with 0 stock
                for(Model m : DataLoad.allModels) m.setStock(oid, 0);
                
                CSVHandler.writeStock(DataLoad.allModels); // Update stock file structure
                refreshOutletCombo();
                loadTableData();
                JOptionPane.showMessageDialog(this, "Outlet Opened Successfully!");
                break;
                
            case "Employees":
                JOptionPane.showMessageDialog(this, "Use 'Register Employee' page for better validation.");
                break;
                
            default:
                JOptionPane.showMessageDialog(this, "Adding rows here is not supported. Use specific functions.");
        }
    }

    // --- SAVE LOGIC ---
    private void saveCurrentData() {
        if (dataTable.isEditing()) dataTable.getCellEditor().stopCellEditing();

        try {
            switch (currentCategory) {
                case "Employees":
                    DataLoad.allEmployees.clear();
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        Employee e = new Employee(
                            (String)tableModel.getValueAt(i,0), 
                            (String)tableModel.getValueAt(i,1), 
                            (String)tableModel.getValueAt(i,2),
                            (String)tableModel.getValueAt(i,3) 
                        );
                        DataLoad.allEmployees.add(e);
                        
                        // === SYNC DATABASE ===
                        DatabaseHandler.saveEmployee(e); 
                    }
                    CSVHandler.writeEmployees(DataLoad.allEmployees); // Backup
                    break;

                case "Stock Inventory":
                    String selectedOutlet = (String) outletFilter.getSelectedItem();
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        String mCode = (String) tableModel.getValueAt(i, 0);
                        double price = Double.parseDouble(tableModel.getValueAt(i, 1).toString());
                        int qty = Integer.parseInt(tableModel.getValueAt(i, 2).toString());

                        for (Model m : DataLoad.allModels) {
                            if (m.getModelCode().equals(mCode)) {
                                m.setPrice(price); // Requires setPrice() in Model.java
                                
                                // Only update specific stock if not "All Outlets"
                                if (!"All Outlets".equals(selectedOutlet)) {
                                    m.setStock(selectedOutlet, qty);
                                }
                                
                                // === SYNC DATABASE ===
                                DatabaseHandler.saveModel(m);
                            }
                        }
                    }
                    CSVHandler.writeStock(DataLoad.allModels);
                    break;

                case "Attendance":
                    DataLoad.allAttendance.clear();
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        // 1. Create Base Object
                        Attendance a = new Attendance(
                            (String)tableModel.getValueAt(i,0), // ID
                            (String)tableModel.getValueAt(i,1), // Name
                            (String)tableModel.getValueAt(i,2), // Date
                            (String)tableModel.getValueAt(i,3)  // Clock In
                        );
                        
                        // 2. FIX: Capture "Clock Out" (Column 4) which was missing!
                        Object clockOutObj = tableModel.getValueAt(i, 4);
                        if (clockOutObj != null) {
                            String clockOut = clockOutObj.toString();
                            if (!clockOut.equals("-") && !clockOut.isEmpty()) {
                                a.setClockOutTime(clockOut);
                            }
                        }

                        DataLoad.allAttendance.add(a);
                        
                        // === SYNC DATABASE ===
                        DatabaseHandler.saveAttendance(a);
                    }
                    CSVHandler.writeAttendance(DataLoad.allAttendance);
                    break;
                    
                case "Sales Records":
                      JOptionPane.showMessageDialog(this, "Sales records are read-only to ensure data integrity.");
                      break;
            }
            JOptionPane.showMessageDialog(this, "Data Saved Successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteSelectedRow() {
        int row = dataTable.getSelectedRow();
        if (row == -1) return;
        
        int modelRow = dataTable.convertRowIndexToModel(row);
        
        // Get the ID (Column 0) BEFORE removing the row
        String idOrCode = (String) tableModel.getValueAt(modelRow, 0);
        
        if (currentCategory.equals("Employees")) {
             // 1. Remove from RAM
             DataLoad.allEmployees.removeIf(e -> e.getId().equals(idOrCode));
             // 2. Remove from CSV
             CSVHandler.writeEmployees(DataLoad.allEmployees);
             // 3. Remove from DATABASE (Fixed)
             DatabaseHandler.deleteEmployee(idOrCode);
             
        } else if (currentCategory.equals("Stock Inventory")) {
             DataLoad.allModels.removeIf(m -> m.getModelCode().equals(idOrCode));
             CSVHandler.writeStock(DataLoad.allModels);
             // 3. Remove from DATABASE (Fixed)
             DatabaseHandler.deleteModel(idOrCode);
        }
        
        // Remove from UI Table
        tableModel.removeRow(modelRow);
    }

    private void filterData() {
        String text = searchField.getText().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        dataTable.setRowSorter(sorter);
        if (text.trim().length() == 0) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
    }

    private void styleActionBtn(JButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
    }
}