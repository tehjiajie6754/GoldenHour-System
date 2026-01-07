package com.goldenhour.gui.dashboard;

import com.goldenhour.gui.admin.DatabaseViewerPanel;
import com.goldenhour.gui.admin.RegisterEmployeePanel;
import com.goldenhour.gui.auth.LoginFrame;
import com.goldenhour.gui.common.BackgroundPanel;
import com.goldenhour.gui.common.SidebarButton;
import com.goldenhour.gui.hr.AttendancePanel;
import com.goldenhour.gui.inventory.StockOperationsPanel;
import com.goldenhour.gui.inventory.StockPanel;
import com.goldenhour.gui.pos.POSPanel;
import com.goldenhour.gui.pos.SalesHistoryPanel;
import com.goldenhour.service.loginregister.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MainDashboardFrame - Primary application window and navigation hub.
 *
 * This is the main GUI frame that serves as the central hub for the GoldenHour System.
 * It provides a sidebar-based navigation system with role-based access control,
 * allowing different user types (Manager, Staff) to access appropriate features.
 *
 * Key Features:
 * - Sidebar navigation with role-based menu items
 * - CardLayout-based panel switching for smooth transitions
 * - Manager-only administrative functions (Register Employee, Manage Database)
 * - Staff-level functions (Attendance, Stock, POS, Sales History)
 * - Automatic logout and session management
 * - Responsive UI with consistent styling
 *
 * The frame manages panel instances and coordinates data synchronization between
 * different components, particularly between registration and database viewing panels.
 *
 * @author GoldenHour System Team
 */
public class MainDashboardFrame extends JFrame {

    // Main UI components
    private JPanel mainContentPanel;        // Container for different panels using CardLayout
    private CardLayout cardLayout;          // Layout manager for panel switching
    private JPanel sidebar;                 // Left sidebar with navigation buttons

    // Navigation state
    private List<SidebarButton> navButtons = new ArrayList<>();  // List of all navigation buttons

    // Panel references for inter-panel communication
    private DatabaseViewerPanel databaseViewerPanel;  // Reference for data synchronization

    /**
     * Constructor - Initializes the main dashboard frame with sidebar navigation and content panels.
     *
     * Sets up the complete UI structure including:
     * - Window properties (title, size, icon)
     * - Sidebar with navigation buttons (role-based visibility)
     * - Main content area with CardLayout for panel switching
     * - Panel instances for all application features
     * - Inter-panel communication setup (database viewer reference for registration panel)
     */
    public MainDashboardFrame() {
        setTitle("Golden Hour System");
        setSize(1280, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        try {
            ImageIcon appIcon = new ImageIcon("goldenhour\\image\\app_icon_1.png");
            setIconImage(appIcon.getImage());
        } catch (Exception e) {
            System.out.println("App Icon not found.");
        }

        // === 1. SIDEBAR SETUP ===
        sidebar = new JPanel();
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(260, 800));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        // Add a shadow/border on the right
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230, 230, 230)));

        // -- Header (Text Version - Crisp & Professional) --
        // Use FlowLayout CENTER to ensure the text sits right in the middle horizontally
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
        header.setBackground(Color.WHITE);
        
        // STRICT SIZE LOCK: prevents it from growing or shrinking
        Dimension headerSize = new Dimension(260, 60);
        header.setPreferredSize(headerSize);
        header.setMinimumSize(headerSize);
        header.setMaximumSize(headerSize);
        
        // CRITICAL FIX: Align the panel itself to the LEFT so it stacks perfectly with buttons
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Using HTML to do Dual-Color Text matching your logo
        JLabel logoLabel = new JLabel("<html><span style='color:#FFC107'>Golden</span><span style='color:#344767'>Hour</span></html>");
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 26)); 
        
        header.add(logoLabel);
        sidebar.add(header);

        // -- Navigation Items (Using Unicode Icons) --
        sidebar.add(createNavButton("Dashboard", "HOME", "⊞", true)); // Default Active
        sidebar.add(Box.createVerticalStrut(10));
        
        sidebar.add(createLabel("MANAGEMENT"));
        sidebar.add(createNavButton("Attendance", "ATTENDANCE", "🕒", false));
        sidebar.add(createNavButton("Stock Inventory", "STOCK", "📦", false));
        sidebar.add(createNavButton("Stock Operations", "STOCK_OPS", "⇄", false));

        // === NEW: MANAGER-ONLY SECTION ===
        // Check if current user is a Manager
        if (AuthService.getCurrentUser() != null && 
            "Manager".equalsIgnoreCase(AuthService.getCurrentUser().getRole())) {
            
            sidebar.add(Box.createVerticalStrut(10));
            sidebar.add(createLabel("ADMIN"));
            // Add the new button
            sidebar.add(createNavButton("Register Employee", "REGISTER_EMP", "👤", false));
            sidebar.add(createNavButton("Manage Database", "DB_VIEWER", "🗄️", false));
        }
        // =================================

        sidebar.add(Box.createVerticalStrut(10));

        sidebar.add(createLabel("SALES"));
        sidebar.add(createNavButton("Point of Sale", "POS", "🛒", false));
        sidebar.add(createNavButton("Sales History", "SALES_HIST", "📜", false));

        // -- Push Logout to Bottom --
        sidebar.add(Box.createVerticalGlue());

        // -- Logout Button --
        SidebarButton logoutBtn = new SidebarButton("Log Out", "➜");
        logoutBtn.setMaximumSize(new Dimension(240, 50));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT); // Align logout too
        logoutBtn.setForeground(new Color(234, 6, 6)); // Red
        logoutBtn.addActionListener(e -> {
            dispose();
            AuthService.logout();
            new LoginFrame().setVisible(true);
        });
        sidebar.add(logoutBtn);
        sidebar.add(Box.createVerticalStrut(30)); // Bottom padding

        // === 2. MAIN CONTENT AREA (MODIFIED FOR BACKGROUND) ===
        cardLayout = new CardLayout();
        // Use BackgroundPanel here so the gaps between cards also show the image
        mainContentPanel = new BackgroundPanel(); 
        mainContentPanel.setLayout(cardLayout);
        mainContentPanel.setBorder(new EmptyBorder(0,0,0,0)); // Remove padding here, let panels handle it

        // Add Pages
        mainContentPanel.add(new HomePanel(), "HOME");
        mainContentPanel.add(new AttendancePanel(), "ATTENDANCE");
        mainContentPanel.add(new StockPanel(), "STOCK");
        mainContentPanel.add(new POSPanel(), "POS");
        mainContentPanel.add(new StockOperationsPanel(), "STOCK_OPS");
        mainContentPanel.add(new SalesHistoryPanel(), "SALES_HIST");
        // === ADD THE NEW PANEL TO CARD LAYOUT ===
        databaseViewerPanel = new DatabaseViewerPanel();
        mainContentPanel.add(new RegisterEmployeePanel(databaseViewerPanel), "REGISTER_EMP");
        mainContentPanel.add(databaseViewerPanel, "DB_VIEWER");

        add(sidebar, BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);
    }

    private Component createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(new Color(160, 160, 160));
        lbl.setBorder(new EmptyBorder(5, 30, 5, 0)); // Align with text
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT); // Critical for BoxLayout
        return lbl;
    }

    /**
     * Creates a navigation button for the sidebar with proper styling and event handling.
     *
     * @param text Display text for the button
     * @param cardName Identifier for the corresponding panel in CardLayout
     * @param icon Unicode icon character for visual representation
     * @param active Whether this button should be initially active/selected
     * @return Configured SidebarButton with click handler for panel switching
     */
    private JButton createNavButton(String text, String cardName, String icon, boolean active) {
        SidebarButton btn = new SidebarButton(text, icon);
        btn.setMaximumSize(new Dimension(240, 50)); // Fixed width, nice height
        btn.setAlignmentX(Component.LEFT_ALIGNMENT); // Critical for BoxLayout
        btn.setActive(active);

        navButtons.add(btn); // Add to list so we can reset them later

        btn.addActionListener(e -> {
            // Reset all navigation buttons to inactive state
            for (SidebarButton b : navButtons) {
                b.setActive(false);
            }
            // Set clicked button to active state
            btn.setActive(true);

            // Switch to the corresponding panel
            cardLayout.show(mainContentPanel, cardName);

            // Special handling: refresh database viewer when navigating to it
            // This ensures the latest data is displayed
            if ("DB_VIEWER".equals(cardName) && databaseViewerPanel != null) {
                databaseViewerPanel.refreshData();
            }
        });

        return btn;
    }

    // Method to update sidebar highlighting when navigating programmatically
    public void updateSidebarActive(String cardName) {
        for (SidebarButton btn : navButtons) {
            // Find the button with matching card name by checking its text
            if ((cardName.equals("HOME") && btn.getText().equals("Dashboard")) ||
                (cardName.equals("ATTENDANCE") && btn.getText().equals("Attendance")) ||
                (cardName.equals("STOCK") && btn.getText().equals("Stock Inventory")) ||
                (cardName.equals("STOCK_OPS") && btn.getText().equals("Stock Operations")) ||
                (cardName.equals("SALES") && btn.getText().equals("Sales")) ||
                (cardName.equals("REGISTER_EMP") && btn.getText().equals("Register Employee")) ||
                (cardName.equals("DB_VIEWER") && btn.getText().equals("Manage Database"))) {
                btn.setActive(true);
            } else {
                btn.setActive(false);
            }
        }
    }
}
