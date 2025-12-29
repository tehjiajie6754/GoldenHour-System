package com.goldenhour.gui.common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SidebarButton extends JButton {
    private boolean isActive = false;
    private Color activeColor = new Color(23, 193, 232); // Cyan Blue (Soft UI)
    private Color activeText = Color.WHITE;
    private Color inactiveText = new Color(103, 116, 142); // Muted Grey
    private Color hoverBg = new Color(245, 245, 245);
    private String iconSymbol;

    public SidebarButton(String text, String iconSymbol) {
        super(text);
        this.iconSymbol = iconSymbol;
        
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setHorizontalAlignment(SwingConstants.LEFT);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 20, 10, 10)); // Padding
        setForeground(inactiveText);

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!isActive) setBackground(hoverBg);
            }
            public void mouseExited(MouseEvent e) {
                if (!isActive) setBackground(Color.WHITE);
            }
        });
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (isActive) {
            setForeground(activeText);
        } else {
            setForeground(inactiveText);
            setBackground(Color.WHITE);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw Background (Rounded if Active)
        if (isActive) {
            g2.setColor(activeColor);
            g2.fillRoundRect(10, 0, getWidth() - 20, getHeight(), 15, 15); // Rounded Card
        } else if (getModel().isRollover()) {
            g2.setColor(hoverBg);
            g2.fillRoundRect(10, 0, getWidth() - 20, getHeight(), 15, 15);
        }

        // 2. Draw Icon (Left Side)
        // We draw a small white/grey box for the icon background
        Color iconBg = isActive ? Color.WHITE : new Color(230, 230, 240);
        g2.setColor(iconBg);
        g2.fillRoundRect(5, (getHeight()-30)/2, 30, 30, 10, 10); // Small icon box

        // Draw the Unicode Icon Symbol
        g2.setColor(isActive ? activeColor : new Color(58, 65, 111));
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        FontMetrics fm = g2.getFontMetrics();
        int iconX = 5 + (30 - fm.stringWidth(iconSymbol)) / 2;
        int iconY = (getHeight() - 30) / 2 + ((30 - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(iconSymbol, iconX, iconY);

        // 3. Draw Text (Shifted Right)
        g2.setColor(getForeground());
        g2.setFont(getFont());
        fm = g2.getFontMetrics();
        int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), 35, textY);
    }
}