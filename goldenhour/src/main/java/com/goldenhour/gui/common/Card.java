package com.goldenhour.gui.common;

import javax.swing.*;
import java.awt.*;

public class Card extends JPanel {
    private Color backgroundColor;
    private int cornerRadius = 20;

    public Card(Color bgColor) {
        this.backgroundColor = bgColor;
        setOpaque(false); // Transparent so we can draw custom rounded shape
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the rounded panel
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        
        super.paintComponent(g);
    }
}