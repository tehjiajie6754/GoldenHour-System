package com.goldenhour.main;

import com.goldenhour.dataload.DataLoad;
import com.goldenhour.gui.auth.LoginFrame;

import javax.swing.SwingUtilities;

public class MainGUI {
    public static void main(String[] args) {
        //Send auto email to headquarters at 10pm daily
        AutoEmail.startDailyScheduler();
        
        // 1. Load Data (Same as Console!)
        System.out.println("Starting GUI Application...");
        DataLoad.loadAllData();

        // 2. Launch GUI
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}