package com.goldenhour.gui.admin;

import com.goldenhour.categories.Employee;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.gui.common.BackgroundPanel;
import com.goldenhour.gui.common.Card;
import com.goldenhour.storage.CSVHandler;
import com.goldenhour.storage.DatabaseHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * RegisterEmployeePanel - GUI panel for registering new employees in the system.
 *
 * This panel provides a form interface for managers to create new employee accounts.
 * It validates input, checks for duplicate IDs, and saves the employee data to both
 * the database and CSV files. After successful registration, it automatically refreshes
 * the database viewer panel to show the new employee immediately.
 *
 * Features:
 * - Input validation for required fields
 * - Duplicate ID checking
 * - Role selection (Full-time, Part-time, Manager)
 * - Automatic data synchronization with database viewer
 *
 * @author GoldenHour System Team
 */
public class RegisterEmployeePanel extends BackgroundPanel {

    // Form input fields
    private JTextField nameField, idField, passField;
    private JComboBox<String> roleCombo;

    // Reference to database viewer for automatic refresh after registration
    private DatabaseViewerPanel databaseViewerPanel;

    /**
     * Constructor with database viewer reference for automatic refresh functionality.
     *
     * @param databaseViewerPanel Reference to the DatabaseViewerPanel to refresh after registration
     */
    public RegisterEmployeePanel(DatabaseViewerPanel databaseViewerPanel) {
        this.databaseViewerPanel = databaseViewerPanel;
        initializeUI();
    }

    /**
     * Default constructor for backward compatibility.
     * Creates panel without automatic refresh functionality.
     */
    public RegisterEmployeePanel() {
        this(null);
    }

    /**
     * Initializes the user interface components and layout.
     * Sets up the registration form with input fields and register button.
     */
    private void initializeUI() {
        setLayout(new GridBagLayout()); // Center the card
        setBorder(new EmptyBorder(30, 30, 30, 30));

        // --- MAIN CARD ---
        Card card = new Card(Color.WHITE);
        card.setPreferredSize(new Dimension(500, 550));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(40, 50, 40, 50));

        // 1. Header
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        JLabel title = new JLabel("Register New Employee");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(52, 71, 103));
        
        JLabel sub = new JLabel("Create a new account for staff access");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(Color.GRAY);
        
        header.add(title);
        header.add(sub);
        card.add(header, BorderLayout.NORTH);

        // 2. Form Fields
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(30, 0, 30, 0));

        nameField = createField("Full Name");
        idField = createField("Employee ID (e.g., C6013)");
        passField = createField("Set Password");
        
        // Role Dropdown
        JLabel roleLbl = new JLabel("Role");
        roleLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        roleLbl.setForeground(new Color(52, 71, 103));
        roleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        roleCombo = new JComboBox<>(new String[]{"Full-time", "Part-time", "Manager"});
        roleCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        roleCombo.setBackground(Color.WHITE);
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(createLabel("Employee Name"));
        form.add(nameField);
        form.add(Box.createVerticalStrut(15));
        
        form.add(createLabel("Employee ID"));
        form.add(idField);
        form.add(Box.createVerticalStrut(15));
        
        form.add(createLabel("Password"));
        form.add(passField);
        form.add(Box.createVerticalStrut(15));
        
        form.add(roleLbl);
        form.add(Box.createVerticalStrut(5));
        form.add(roleCombo);

        card.add(form, BorderLayout.CENTER);

        // 3. Register Button
        JButton regBtn = new JButton("Register Employee");
        regBtn.setBackground(new Color(105, 108, 255)); // Sneat Purple
        regBtn.setForeground(Color.WHITE);
        regBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        regBtn.setFocusPainted(false);
        regBtn.setPreferredSize(new Dimension(0, 45));
        regBtn.addActionListener(e -> performRegistration());
        
        card.add(regBtn, BorderLayout.SOUTH);

        add(card);
    }

    /**
     * Handles the employee registration process when the register button is clicked.
     *
     * This method performs the following steps:
     * 1. Validates that all required fields are filled
     * 2. Checks for duplicate employee IDs
     * 3. Creates a new Employee object
     * 4. Saves the employee to both database and CSV files
     * 5. Shows success message and refreshes the database viewer
     * 6. Clears the form fields for the next registration
     */
    private void performRegistration() {
        // Get and trim input values
        String name = nameField.getText().trim();
        String id = idField.getText().trim();
        String pass = passField.getText().trim();
        String role = (String) roleCombo.getSelectedItem();

        // Input validation - ensure all required fields are filled
        if (name.isEmpty() || id.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check for duplicate employee ID to prevent conflicts
        boolean exists = DataLoad.allEmployees.stream().anyMatch(u -> u.getId().equalsIgnoreCase(id));
        if (exists) {
            JOptionPane.showMessageDialog(this, "Employee ID " + id + " already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create new employee object with provided details
        Employee newEmployee = new Employee(id, name, role, pass);

        // Add to in-memory list for immediate access
        DataLoad.allEmployees.add(newEmployee);

        // Persist data to both database and CSV files for redundancy
        DatabaseHandler.saveEmployee(newEmployee);
        CSVHandler.writeEmployees(DataLoad.allEmployees);

        // Show success confirmation to user
        JOptionPane.showMessageDialog(this, "Employee Registered Successfully!\nName: " + name + "\nID: " + id);

        // Refresh database viewer to show new employee immediately
        if (databaseViewerPanel != null) {
            databaseViewerPanel.refreshData();
        }

        // Clear form fields for next registration
        nameField.setText("");
        idField.setText("");
        passField.setText("");
        roleCombo.setSelectedIndex(0);
    }

    // --- Helper Methods for UI Components ---

    /**
     * Creates a styled label for form fields with consistent formatting.
     *
     * @param text The text to display on the label
     * @return A configured JLabel with proper styling
     */
    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(52, 71, 103));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /**
     * Creates a styled text field for form input with consistent appearance.
     *
     * @param placeholder Placeholder text (not actually used as placeholder, just for documentation)
     * @return A configured JTextField with proper styling and border
     */
    private JTextField createField(String placeholder) {
        JTextField f = new JTextField();
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(217, 222, 227)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }
}