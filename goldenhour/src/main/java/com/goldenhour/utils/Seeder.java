package com.goldenhour.utils;

import com.goldenhour.categories.Sales;
import com.goldenhour.storage.DatabaseHandler;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

import java.util.Random;

public class Seeder {

    public static void seed2025Data() {
        //System.out.println("Checking for 2025 data...");

        Random random = new Random();
        String[] models = { "M-001", "M-002", "M-003", "M-004" }; // Dummy models
        String[] methods = { "Cash", "Card", "E-Wallet" };

        // Generate a nice curve:
        // Q1 (Jan-Mar): Rising
        // Q2 (Apr-Jun): Dip
        // Q3 (Jul-Sep): High Peak
        // Q4 (Oct-Dec): Steady high

        boolean anySeeded = false;

        // Only seed Jan (1) to Oct (10) as per user request to fill gaps
        // BUT actually, user said "we got sales on Nov and Dec", so we should check ALL
        // months
        // If a month is missing, we populate it.
        for (int month = 1; month <= 12; month++) {
            if (hasDataForMonth(2025, month)) {
                //System.out.println("Data exists for 2025-" + month + ". Skipping.");
                continue;
            }

            //System.out.println("Seeding data for 2025-" + month + "...");
            anySeeded = true;

            int salesCount = 10 + random.nextInt(10); // Base sales per month

            // Adjust count based on "seasonality" to make graph look nice
            if (month <= 3)
                salesCount += 5 + month * 2;
            else if (month <= 6)
                salesCount += 5;
            else if (month <= 9)
                salesCount += 15 + (month - 6) * 3; // Peak
            else
                salesCount += 20; // End year high

            int daysInMonth = LocalDate.of(2025, month, 1).lengthOfMonth();

            for (int i = 0; i < salesCount; i++) {
                int day = 1 + random.nextInt(daysInMonth);
                String date = String.format("2025-%02d-%02d", month, day);

                int hour = 10 + random.nextInt(10); // 10am to 8pm
                int min = random.nextInt(60);
                String time = String.format("%02d:%02d %s", (hour > 12 ? hour - 12 : hour), min,
                        (hour >= 12 ? "pm" : "am"));

                String model = models[random.nextInt(models.length)];
                int qty = 1 + random.nextInt(3);
                double price = 50.0 + random.nextDouble() * 100.0; // Random price between 50 and 150
                double subtotal = price * qty;

                Sales sale = new Sales();
                sale.setDate(date);
                sale.setTime(time);
                sale.setCustomerName("Guest " + (1000 + random.nextInt(9000)));
                sale.setModel(model);
                sale.setQuantity(qty);
                sale.setSubtotal(subtotal);
                sale.setTransactionMethod(methods[random.nextInt(methods.length)]);
                sale.setEmployee("System Seeder");
                sale.setOutletCode("HQ");
                sale.setEmployeeId("SYS");

                DatabaseHandler.saveSale(sale);
            }
        }

        if (anySeeded) {
            //System.out.println("2025 data seeding complete.");
        } else {
           // System.out.println("2025 data checks complete - everything is populated.");
        }
    }

    private static boolean hasDataForMonth(int year, int month) {
        String monthStr = String.format("%02d", month);
        String datePattern = year + "-" + monthStr + "-%";
        String sql = "SELECT COUNT(*) FROM sales WHERE date LIKE '" + datePattern + "'";
        try (Connection conn = DatabaseHandler.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
