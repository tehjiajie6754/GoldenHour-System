package com.goldenhour.gui.dashboard;

import com.goldenhour.service.salessys.SalesCalculator;
import com.goldenhour.service.salessys.ProductStats;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

public class TopProductPanel extends JPanel {

    public TopProductPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15)); // Margin around the whole box

        // 1. Title Header
        JLabel lblTitle = new JLabel("Top 5 Most Sold Products");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitle.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(lblTitle, BorderLayout.NORTH);

        // 2. The List Container
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        // 3. Column Headers (Like the reference image)
        JPanel headerRow = createRowPanel("Rank", "Product Code", "Qty Sold", "Total Revenue", true);
        listPanel.add(headerRow);

        // 4. Fetch Data & Create Rows
        List<ProductStats> top5 = SalesCalculator.getTop5Products();
        
        if (top5.isEmpty()) {
            JLabel noData = new JLabel("No sales data available.");
            noData.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(noData);
        } else {
            int rank = 1;
            for (ProductStats p : top5) {
                // Determine badge color based on rank
                Color badgeColor = (rank == 1) ? new Color(255, 215, 0) : // Gold
                                   (rank == 2) ? new Color(192, 192, 192) : // Silver
                                   (rank == 3) ? new Color(205, 127, 50) :  // Bronze
                                   new Color(230, 230, 230); // Gray for others
                
                JPanel row = createRowPanel(
                    String.valueOf(rank), 
                    p.getModelCode(), 
                    String.valueOf(p.getTotalQty()) + " units", 
                    String.format("RM %.2f", p.getTotalRevenue()), 
                    false
                );
                listPanel.add(row);
                rank++;
            }
        }

        add(listPanel, BorderLayout.CENTER);
    }

    private JPanel createRowPanel(String col1, String col2, String col3, String col4, boolean isHeader) {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0)); // 1 row, 4 cols
        row.setBackground(Color.WHITE);
        row.setBorder(new MatteBorder(0, 0, 1, 0, new Color(240, 240, 240))); // Bottom line separator
        row.setPreferredSize(new Dimension(400, 40));
        row.setMaximumSize(new Dimension(2000, 40)); // Prevent it from getting too tall

        Font font = isHeader ? new Font("SansSerif", Font.BOLD, 12) : new Font("SansSerif", Font.PLAIN, 13);
        Color textColor = isHeader ? Color.GRAY : Color.BLACK;

        // Helper to add label
        row.add(createLabel(col1, font, textColor));
        row.add(createLabel(col2, font, textColor));
        row.add(createLabel(col3, font, textColor));
        
        // Make the revenue green like the image
        JLabel lblRev = createLabel(col4, font, isHeader ? textColor : new Color(34, 139, 34)); 
        row.add(lblRev);

        return row;
    }

    private JLabel createLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }
}