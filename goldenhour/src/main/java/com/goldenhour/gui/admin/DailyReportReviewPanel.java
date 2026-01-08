package com.goldenhour.gui.admin;

import com.goldenhour.categories.DailySalesSummary;
import com.goldenhour.gui.common.BackgroundPanel;
import com.goldenhour.gui.common.Card;
import com.goldenhour.service.autoemail.AutoEmail;
import com.goldenhour.service.salessys.SalesCalculator;
import com.goldenhour.utils.TimeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * DailyReportReviewPanel - Panel for managers to review the daily sales report before sending.
 *
 * This panel displays the daily sales summary data and allows the manager to send the email
 * after reviewing the information. It provides a preview of what will be emailed to headquarters.
 *
 * @author GoldenHour System Team
 */
public class DailyReportReviewPanel extends BackgroundPanel {

    private DailySalesSummary todayStats;
    private String date;
    private double totalSales;
    private Runnable goBackCallback;

    // UI components to update
    private JLabel dateLabel;
    private JLabel totalSalesLabel;
    private JLabel transactionCountLabel;
    private JLabel averageLabel;

    public DailyReportReviewPanel(Runnable goBackCallback) {
        this.goBackCallback = goBackCallback;
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(30, 30, 30, 30));

        // Load today's data
        loadData();

        // Main card
        Card card = new Card(Color.WHITE);
        card.setPreferredSize(new Dimension(600, 500));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(40, 50, 40, 50));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        JLabel title = new JLabel("Daily Sales Report Review");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(52, 71, 103));

        JLabel sub = new JLabel("Review the sales data before sending to Headquarters");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(Color.GRAY);

        header.add(title);
        header.add(sub);
        card.add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(30, 0, 30, 0));

        // Date
        dateLabel = createInfoLabel("Date: " + date);
        content.add(dateLabel);
        content.add(Box.createVerticalStrut(15));

        // Total Sales
        totalSalesLabel = createInfoLabel("Total Sales: RM" + String.format("%.2f", totalSales));
        content.add(totalSalesLabel);
        content.add(Box.createVerticalStrut(15));

        // Additional stats if available
        if (todayStats != null) {
            transactionCountLabel = createInfoLabel("Number of Transactions: " + todayStats.getTransactionCount());
            content.add(transactionCountLabel);
            content.add(Box.createVerticalStrut(15));
            averageLabel = createInfoLabel("Average Transaction: RM" + String.format("%.2f",
                todayStats.getTransactionCount() > 0 ? totalSales / todayStats.getTransactionCount() : 0));
            content.add(averageLabel);
        }

        card.add(content, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttons.setOpaque(false);

        JButton sendBtn = new JButton("📧 Send Report");
        sendBtn.setBackground(new Color(40, 199, 111)); // Green
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        sendBtn.setFocusPainted(false);
        sendBtn.setPreferredSize(new Dimension(150, 45));
        sendBtn.addActionListener(e -> sendReport());

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(new Color(234, 84, 85)); // Red
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setPreferredSize(new Dimension(120, 45));
        cancelBtn.addActionListener(e -> goBack());

        buttons.add(sendBtn);
        buttons.add(cancelBtn);

        card.add(buttons, BorderLayout.SOUTH);

        add(card);
    }

    private void loadData() {
        todayStats = SalesCalculator.getSummary(null, null);
        date = TimeUtils.getDate();
        totalSales = todayStats.getGrandTotal();
    }

    /**
     * Public method to refresh the displayed data.
     * Call this when navigating to the panel to ensure latest data is shown.
     */
    public void refreshData() {
        loadData(); // Reload data
        // Update UI labels
        dateLabel.setText("Date: " + date);
        totalSalesLabel.setText("Total Sales: RM" + String.format("%.2f", totalSales));
        if (todayStats != null && transactionCountLabel != null && averageLabel != null) {
            transactionCountLabel.setText("Number of Transactions: " + todayStats.getTransactionCount());
            averageLabel.setText("Average Transaction: RM" + String.format("%.2f",
                todayStats.getTransactionCount() > 0 ? totalSales / todayStats.getTransactionCount() : 0));
        }
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        label.setForeground(new Color(52, 71, 103));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void sendReport() {
        // Send the report
        AutoEmail.sendDailyReport(true);

        // Go back to dashboard
        goBack();
    }

    private void goBack() {
        if (goBackCallback != null) {
            goBackCallback.run();
        }
    }
}