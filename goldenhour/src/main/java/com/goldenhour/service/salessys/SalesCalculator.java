package com.goldenhour.service.salessys;

import com.goldenhour.categories.Sales;
import com.goldenhour.categories.DailySalesSummary;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.storage.DatabaseHandler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SalesCalculator {

    private static final DateTimeFormatter DB_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * The Main Method you will call.
     * * @param targetDate  Pass null to use Today's date automatically.
     * @param targetOutlet Pass null or "All" to get Total Company Sales.
     * @return DailySalesSummary object with all calculated data.
     */
    public static DailySalesSummary getSummary(LocalDate targetDate, String targetOutlet) {
        // 1. Handle Defaults
        if (targetDate == null) targetDate = LocalDate.now();
        if (targetOutlet == null) targetOutlet = "All";

        // 2. Ensure Data is Loaded
        if (DataLoad.allSales == null || DataLoad.allSales.isEmpty()) {
            DataLoad.allSales = DatabaseHandler.fetchAllSales();
        }

        // 3. Filter the List
        String finalDateStr = targetDate.format(DB_DATE_FMT);
        String finalOutlet = targetOutlet;

        List<Sales> filteredSales = DataLoad.allSales.stream()
            // Filter by Date
            .filter(s -> s.getDate().equals(finalDateStr))
            // Filter by Outlet (Optional)
            .filter(s -> finalOutlet.equals("All") || (s.getOutletCode() != null && s.getOutletCode().equalsIgnoreCase(finalOutlet)))
            .collect(Collectors.toList());

        // 4. Calculate Metrics
        double totalSales = filteredSales.stream()
                .mapToDouble(Sales::getSubtotal)
                .sum();

        int count = filteredSales.size();

        // Group by Payment Method (e.g., Cash: 100.0, Card: 500.0)
        Map<String, Double> breakdown = filteredSales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTransactionMethod() != null ? s.getTransactionMethod() : "Unknown",
                        Collectors.summingDouble(Sales::getSubtotal)
                ));

        // 5. Pack and Return
        return new DailySalesSummary(targetDate, finalOutlet, totalSales, count, breakdown);
    }

    // Add this method to SalesCalculator class
    public static List<ProductStats> getTop5Products() {
        // Ensure data is loaded
        if (DataLoad.allSales == null || DataLoad.allSales.isEmpty()) {
            DataLoad.allSales = DatabaseHandler.fetchAllSales();
        }

        // Group by Model Code and Sum Quantity & Revenue
        Map<String, ProductStats> statsMap = new HashMap<>();

        for (Sales s : DataLoad.allSales) {
            String code = s.getModel(); // Assuming getModel() returns model_code
            statsMap.putIfAbsent(code, new ProductStats(code, 0, 0.0));
            
            ProductStats current = statsMap.get(code);
            current.totalQty += s.getQuantity();
            current.totalRevenue += s.getSubtotal();
        }

        // Convert to list, sort descending by Quantity, and take top 5
        return statsMap.values().stream()
                .sorted((p1, p2) -> Integer.compare(p2.totalQty, p1.totalQty)) // Sort High to Low
                .limit(5)
                .collect(Collectors.toList());
    }
}
