package com.goldenhour.gui.common;

import javax.swing.*;
import java.awt.*;

/**
 * A Base class for all content panels.
 * It automatically draws a background image across the entire panel.
 */
public class BackgroundPanel extends JPanel {
    private Image bgImage;

    public BackgroundPanel() {
        // Ensure the panel is opaque so the background paints correctly
        setOpaque(true);
        
        try {
            // Load the background image path relative to the class path
            String iconPath = "goldenhour\\image\\background.png";
            ImageIcon icon = new ImageIcon(iconPath);
            bgImage = icon.getImage();
        } catch (Exception e) {
            // Fallback if image is missing: just set a nice flat color
            System.err.println("Background image not found. Using fallback color.");
            setBackground(new Color(245, 247, 250)); 
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // If image loaded successfully, draw it stretched to fill the panel
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}