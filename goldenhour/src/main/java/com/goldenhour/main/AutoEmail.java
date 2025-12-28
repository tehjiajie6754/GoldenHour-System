package com.goldenhour.main;

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

public class AutoEmail {
    private static final String SENDER_EMAIL = "25006805@siswa.um.edu.my"; 
    private static final String SENDER_PASSWORD = "rwrhhlerjuskusrg"; //Gmail app password (not login password)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final int TARGET_HOUR = 14; 
    private static final int TARGET_MIN = 29;

    public static void sendDailyReport() {
        // passing null uses "Today" and "All Outlets" automatically
        DailySalesSummary todayStats = SalesCalculator.getSummary(null, null);

        String recipient = "25006805@siswa.um.edu.my";
        String date = TimeUtils.getDate();
        String filePath = "data/receipts/sales_" + date + ".txt";
        double totalSales = todayStats.getGrandTotal();

        // Send the email
        System.out.println("Attempting to send End-of-Day report...");
        sendEmail(recipient, filePath, totalSales, date);
    }

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

    public static void sendEmail(String toAddress, String attachmentPath, double totalSales, String date) {
        // 1. Setup Mail Server Properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);

        // 2. Create a Session with Authentication
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            // 3. Create the Message object
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject("Daily Sales Report: " + date);

            // 4. Create the Multipart body (Text + Attachment)
            Multipart multipart = new MimeMultipart();

            // Part A: The Text Body (Summary)
            BodyPart messageBodyPart = new MimeBodyPart();
            String emailContent = "Headquarters,\n\n"
                    + "Attached is the daily sales receipt file.\n\n"
                    + "--- Sales Summary ---\n"
                    + "Date: " + date + "\n"
                    + "Total Sales Amount: $" + String.format("%.2f", totalSales) + "\n\n"
                    + "Regards,\nStore System Automation";
            messageBodyPart.setText(emailContent);
            multipart.addBodyPart(messageBodyPart);

            // Part B: The Attachment
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachmentPath);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(new File(attachmentPath).getName()); // Set the name of the file
            multipart.addBodyPart(messageBodyPart);

            // 5. Combine and Send
            message.setContent(multipart);
            Transport.send(message);

            System.out.println("Email sent successfully to " + toAddress);

        } catch (MessagingException e) {
            System.err.println("Failed to send email. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

