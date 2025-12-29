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

public class RegisterEmployeePanel extends BackgroundPanel {

    private JTextField nameField, idField, passField;
    private JComboBox<String> roleCombo;

    public RegisterEmployeePanel() {
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

    private void performRegistration() {
        String name = nameField.getText().trim();
        String id = idField.getText().trim();
        String pass = passField.getText().trim();
        String role = (String) roleCombo.getSelectedItem();

        // Validation
        if (name.isEmpty() || id.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if ID exists
        boolean exists = DataLoad.allEmployees.stream().anyMatch(u -> u.getId().equalsIgnoreCase(id));
        if (exists) {
            JOptionPane.showMessageDialog(this, "Employee ID " + id + " already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create & Save
        Employee newEmployee = new Employee(id, name, role, pass);
        DataLoad.allEmployees.add(newEmployee);
        
        // Save to Database & CSV
        DatabaseHandler.saveEmployee(newEmployee);
        CSVHandler.writeEmployees(DataLoad.allEmployees);
        JOptionPane.showMessageDialog(this, "Employee Registered Successfully!\nName: " + name + "\nID: " + id);
        
        // Clear fields
        nameField.setText("");
        idField.setText("");
        passField.setText("");
        roleCombo.setSelectedIndex(0);
    }

    // --- Helpers ---
    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(52, 71, 103));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

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