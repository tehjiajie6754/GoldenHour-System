package com.goldenhour.gui.auth;

import com.goldenhour.gui.dashboard.MainDashboardFrame;
import com.goldenhour.service.loginregister.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginFrame extends JFrame {
    private JTextField userIdField;
    private JPasswordField passwordField;

    // Update these paths to your actual images if needed
    
    private final String LOGO_PATH = "goldenhour\\image\\app_icon_1.png";
    private final String SIDE_IMAGE_PATH = "goldenhour\\image\\side_image.png";

    public LoginFrame() {
        setTitle("Golden Hour System - Login");
        setSize(1000, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main Container: Split 50% Left (Form) | 50% Right (Image)
        setLayout(new GridLayout(1, 2));

        // === 1. LEFT SIDE: LOGIN FORM ===
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(Color.WHITE);
        
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setBackground(Color.WHITE);
        formContainer.setBorder(new EmptyBorder(0, 50, 0, 50)); // Padding sides
        formContainer.setPreferredSize(new Dimension(400, 500)); 

        // -- Frame Icon (Window & Taskbar) --
        try {
            ImageIcon icon = new ImageIcon(LOGO_PATH);
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.out.println("App icon not found.");
        }

        // -- Logo (Resized & Centered inside form) --
        JLabel logoLabel = new JLabel();
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ImageIcon resizedLogo = resizeIcon(LOGO_PATH, 100, 100); 
        if (resizedLogo != null) {
            logoLabel.setIcon(resizedLogo);
        } else {
            logoLabel.setText("GoldenHour");
        }

        // -- Welcome Text --
        JLabel welcome = new JLabel("<html><span style='color:#344767'>Welcome to </span>" +
                "<span style='color:#FFC107'>GoldenHour!</span>" +
                "<span style='color:#344767'> </span></html>");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        welcome.setForeground(new Color(52, 71, 103));
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel sub = new JLabel("Please sign-in to your account");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sub.setForeground(Color.GRAY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        // -- Inputs --
        JLabel userLbl = createLabel("User ID");
        userIdField = createTextField();
        
        JLabel passLbl = createLabel("Password");
        passwordField = createPasswordField();

        // -- Login Button (New Orange Theme) --
        JButton loginBtn = createOrangeButton("Sign in");
        loginBtn.addActionListener(e -> performLogin());

        // -- Assemble Form --
        formContainer.add(logoLabel);
        formContainer.add(Box.createVerticalStrut(20));
        formContainer.add(welcome);
        formContainer.add(Box.createVerticalStrut(5));
        formContainer.add(sub);
        formContainer.add(Box.createVerticalStrut(30));
        
        formContainer.add(userLbl);
        formContainer.add(Box.createVerticalStrut(5));
        formContainer.add(userIdField);
        formContainer.add(Box.createVerticalStrut(15));
        
        formContainer.add(passLbl);
        formContainer.add(Box.createVerticalStrut(5));
        formContainer.add(passwordField);
        formContainer.add(Box.createVerticalStrut(30));
        
        formContainer.add(loginBtn);

        leftPanel.add(formContainer); 
        add(leftPanel);

        // === 2. RIGHT SIDE: FULL IMAGE ===
        ImagePanel rightPanel = new ImagePanel();
        add(rightPanel);
    }

    private void performLogin() {
        String id = userIdField.getText();
        String pass = new String(passwordField.getPassword());

        if (AuthService.login(id, pass)) {
            new MainDashboardFrame().setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid User ID or Password", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- HELPER: Create Orange Themed Button ---
    private JButton createOrangeButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);

        // Define Orange Colors
        Color primaryOrange = new Color(245, 124, 0);  // A nice, modern orange
        Color hoverOrange = new Color(230, 81, 0);    // Slightly darker for hover

        btn.setBackground(primaryOrange);
        btn.setFocusPainted(false); // Remove focus ring
        btn.setBorderPainted(false); // Flat style
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add Hover Effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverOrange);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(primaryOrange);
            }
        });

        // Size and Alignment
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45)); // Full width
        btn.setPreferredSize(new Dimension(0, 45));
        
        return btn;
    }

    // --- HELPER: RESIZE ICON SMOOTHLY ---
    private ImageIcon resizeIcon(String path, int width, int height) {
        try {
            ImageIcon original = new ImageIcon(path);
            if (original.getIconWidth() == -1) return null; 
            Image img = original.getImage();
            Image newImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(newImg);
        } catch (Exception e) {
            return null;
        }
    }

    // --- HELPER COMPONENTS ---
    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(new Color(86, 106, 127));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField createTextField() {
        JTextField t = new JTextField();
        styleField(t);
        return t;
    }

    private JPasswordField createPasswordField() {
        JPasswordField p = new JPasswordField();
        styleField(p);
        return p;
    }

    private void styleField(JTextField f) {
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setPreferredSize(new Dimension(0, 40));
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(217, 222, 227), 1, true),
            new EmptyBorder(5, 10, 5, 10)
        ));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // --- CUSTOM IMAGE PANEL FOR RIGHT SIDE ---
    class ImagePanel extends JPanel {
        private Image img;

        public ImagePanel() {
            try {
                ImageIcon icon = new ImageIcon(SIDE_IMAGE_PATH); 
                img = icon.getImage();
            } catch (Exception e) {
                setBackground(new Color(245, 245, 249));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}