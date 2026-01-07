package com.goldenhour.main;

import com.goldenhour.dataload.DataLoad;
import com.goldenhour.gui.auth.LoginFrame;
import com.goldenhour.service.autoemail.AutoEmail;

import javax.swing.SwingUtilities;

/**
 * MainGUI - Application entry point for the GoldenHour System GUI application.
 *
 * This class serves as the main entry point for the graphical user interface version
 * of the GoldenHour retail management system. It initializes the application by:
 * - Starting the automated email scheduler for daily headquarters reports
 * - Loading all system data (employees, inventory, sales, etc.) from files and database
 * - Launching the login interface on the Event Dispatch Thread
 *
 * The application follows a standard Swing application pattern with proper thread
 * management to ensure UI responsiveness and thread safety.
 *
 * @author GoldenHour System Team
 */
public class MainGUI {

    /**
     * Main method - Application entry point.
     *
     * Initializes the application components in the correct order:
     * 1. Start background services (email scheduler)
     * 2. Load data from persistent storage
     * 3. Launch GUI on EDT (Event Dispatch Thread)
     *
     * @param args Command line arguments (currently unused)
     */
    public static void main(String[] args) {
        // Start automated email service for daily headquarters reports at 10 PM
        AutoEmail.startDailyScheduler();

        // Load all system data (employees, inventory, sales, outlets) from CSV and database
        System.out.println("Starting GUI Application...");
        DataLoad.loadAllData();

        // Launch the login interface on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}