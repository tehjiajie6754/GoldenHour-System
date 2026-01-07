package com.goldenhour.service.autoemail;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.io.File;

import com.goldenhour.categories.DailySalesSummary;
import com.goldenhour.service.salessys.SalesCalculator;
import com.goldenhour.utils.TimeUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.TrayIcon.MessageType;
import java.awt.AWTException;

/**
 * AutoEmail - Daily Sales Report Email System
 *
 * HOW TO SETUP EMAIL:
 * ===================
 *
 * 1. GMAIL SETUP:
 *    - Go to Gmail → Settings → Security → 2-Step Verification → App passwords
 *    - Generate app password for "Mail"
 *    - Copy the 16-character password
 *
 * 2. CHANGE THESE VALUES:
 *    - SENDER_EMAIL: Your Gmail address
 *    - SENDER_PASSWORD: The app password (not your regular password!)
 *
 * 3. TIME SETTING:
 *    - TARGET_HOUR: Hour to send email (22 = 10 PM)
 *    - TARGET_MIN: Minute to send email (15 = 10:15 PM)
 *
 * HOW EMAIL WORKS:
 * ================
 * - App starts → schedules email for TARGET_HOUR:TARGET_MIN daily
 * - At scheduled time → gets sales data → creates email → sends to recipient
 * - Email includes: sales summary + attached sales receipt file
 * - Shows notification in system tray when sent
 *
 * DEPENDENCIES NEEDED:
 * ====================
 * - javax.mail-api-1.6.2.jar
 * - javax.activation-api-1.2.0.jar
 * (Add to classpath or run with Maven)
 *
 * @author GoldenHour System Team
 */
public class AutoEmail {
    /*
     * EMAIL CONFIGURATION - CHANGE THESE VALUES:
     * ==========================================
     *
     * SENDER_EMAIL: Your Gmail address
     * SENDER_PASSWORD: Gmail APP PASSWORD (get from Gmail settings)
     * TARGET_HOUR/TARGET_MIN: Time to send email (22:15 = 10:15 PM)
     */
    private static final String SENDER_EMAIL = "25006805@siswa.um.edu.my";
    //private static final String SENDER_EMAIL = "25006805@siswa.um.edu.my";
    private static final String SENDER_PASSWORD = "xnyd pxyb xrfg rjsw"; // Gmail app password (not login password)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final int TARGET_HOUR = 22;
    private static final int TARGET_MIN = 15;

    /**
     * Sends the daily sales report email.
     * Called automatically every day at scheduled time, or manually for testing.
     */
    public static void sendDailyReport() {
        // Get today's sales data (null parameters = use current date and all outlets)
        DailySalesSummary todayStats = SalesCalculator.getSummary(null, null);

        String recipient = "25006739@siswa.um.edu.my";
        String date = TimeUtils.getDate();
        String filePath = "data/receipts/sales_" + date + ".txt";
        double totalSales = todayStats.getGrandTotal();

        // Send the email
        System.out.println("Attempting to send End-of-Day report...");
        sendEmail(recipient, filePath, totalSales, date);
    }

    /**
     * Starts the automated daily email scheduler.
     * Called once when app starts. Schedules emails for TARGET_HOUR:TARGET_MIN daily.
     */
    public static void startDailyScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // 1. Calculate how long to wait until 10:00 PM
        long initialDelay = computeNextDelay(TARGET_HOUR, TARGET_MIN);

        // 2. Schedule the task
        // args: (The Task, Initial Delay, Repeat every 24 hours, Time Unit)
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    sendDailyReport();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, initialDelay, 24 * 60 * 60, TimeUnit.SECONDS);
    }

    private static long computeNextDelay(int targetHour, int targetMin) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(targetHour).withMinute(targetMin).withSecond(0);

        if (now.compareTo(nextRun) > 0) {
            // If 10:00 PM has already passed today, schedule for tomorrow
            nextRun = nextRun.plusDays(1);
        }

        Duration duration = Duration.between(now, nextRun);
        return duration.getSeconds();
    }

    /**
     * Sends an email with attachment using Gmail SMTP.
     * Creates email with sales summary and attaches the daily sales file.
     *
     * @param toAddress Email recipient address
     * @param attachmentPath Path to file to attach
     * @param totalSales Total sales amount for email body
     * @param date Date string for subject and content
     */
    public static void sendEmail(String toAddress, String attachmentPath, double totalSales, String date) {
        // 1. Setup Mail Server Properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);

        // 2. Create a Session with Authentication
        // This creates a secure connection to Gmail using your app password
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            // 3. Create the Message object (email container)
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject("Daily Sales Report: " + date);

            // 4. Create the Multipart body (Text + Attachment)
            // Multipart allows mixing text content with file attachments
            Multipart multipart = new MimeMultipart();

            // Part A: The Text Body (Summary)
            // This creates the main email content with sales information
            BodyPart messageBodyPart = new MimeBodyPart();
            String emailContent = "Headquarters,\n\n"
                    + "Attached is the daily sales receipt file.\n\n"
                    + "--- Sales Summary ---\n"
                    + "Date: " + date + "\n"
                    + "Total Sales Amount: RM" + String.format("%.2f", totalSales) + "\n\n"
                    + "Regards,\nStore System Automation";
            messageBodyPart.setText(emailContent);
            multipart.addBodyPart(messageBodyPart);

            // Part B: The Attachment
            // This attaches the actual sales receipt file to the email
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachmentPath);  // Load the file
            messageBodyPart.setDataHandler(new DataHandler(source)); // Attach it to email
            messageBodyPart.setFileName(new File(attachmentPath).getName()); // Use original filename
            multipart.addBodyPart(messageBodyPart);

            // 5. Combine and Send
            // Set the multipart content as the email body and send it
            message.setContent(multipart);
            Transport.send(message);  // This actually sends the email via SMTP

            // SUCCESS NOTIFICATION: Show system tray popup when email sent successfully
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                // Load app icon for the notification
                Image image = Toolkit.getDefaultToolkit().createImage("goldenhour\\image\\app_icon_1.png");

                TrayIcon trayIcon = new TrayIcon(image, "GoldenHour POS");
                trayIcon.setImageAutoSize(true);

                try {
                    tray.add(trayIcon);
                    trayIcon.displayMessage("Daily Report",
                        "✅ Sales report successfully emailed to Headquarters.",
                        MessageType.INFO);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            System.out.println("Email sent successfully to " + toAddress);

        } catch (MessagingException e) {
            // ERROR NOTIFICATION: Show system tray popup when email fails
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                // Ensure this image path is correct, or the tray icon might be blank
                Image image = Toolkit.getDefaultToolkit().createImage("goldenhour\\image\\app_icon_1.png"); 
                
                TrayIcon trayIcon = new TrayIcon(image, "GoldenHour POS");
                trayIcon.setImageAutoSize(true);
                
                try {
                    tray.add(trayIcon);
                    // Show Error Bubble
                    trayIcon.displayMessage("Auto-Email Failed", 
                        "❌ Could not send daily report.\nError: " + e.getMessage(), 
                        MessageType.ERROR);
                } catch (AWTException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("Failed to send email. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

