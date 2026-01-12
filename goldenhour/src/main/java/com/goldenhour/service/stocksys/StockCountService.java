package com.goldenhour.service.stocksys;

import com.goldenhour.categories.Attendance;
import com.goldenhour.categories.Employee;
import com.goldenhour.categories.Model;
import com.goldenhour.categories.Outlet;
import com.goldenhour.utils.TimeUtils;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.service.loginregister.AuthService;
import com.goldenhour.storage.CSVHandler;

import java.util.*;


public class StockCountService {

    public static void performStockCount(String period) {
        Scanner sc = new Scanner(System.in);
        
        // Check if user is logged in
        Employee currentUser = AuthService.getCurrentUser();
        if (currentUser == null) {
            System.out.println("\u001B[31mError: No user logged in.\u001B[0m");
            return;
        }
        
        // Reload attendance from CSV to get the latest data
        DataLoad.allAttendance = CSVHandler.readAttendance();
        
        // Check if user has clocked in today
        String today = TimeUtils.getDate();
        Attendance todayAttendance = null;
        
        for (Attendance a : DataLoad.allAttendance) {
            if (a.getEmployeeId().equals(currentUser.getId()) && 
                a.getDate().equals(today) && 
                a.getClockInTime() != null) {
                todayAttendance = a;
                break;
            }
        }
        
        if (todayAttendance == null || todayAttendance.getOutletCode() == null) {
            System.out.println("\u001B[31mError: You must sign attendance first before performing stock count.\u001B[0m");
            System.out.println("Please clock in at your outlet to continue.");
            return;
        }
        
        // Check if user has already clocked out
        if (todayAttendance.getClockOutTime() != null && !todayAttendance.getClockOutTime().isEmpty()) {
            System.out.println("\u001B[31mError: You have already clocked out today.\u001B[0m");
            System.out.println("Stock count can only be performed during your shift.");
            return;
        }
        
        // Get the outlet from attendance
        String outletCode = todayAttendance.getOutletCode();
        Outlet currentOutlet = null;
        
        for (Outlet o : DataLoad.allOutlets) {
            if (o.getOutletCode().equals(outletCode)) {
                currentOutlet = o;
                break;
            }
        }
        
        if (currentOutlet == null) {
            System.out.println("\u001B[31mError: Outlet not found.\u001B[0m");
            return;
        }
        
        List<Model> stockList = DataLoad.allModels;

        int totalChecked = 0;
        int tallyCorrect = 0;
        int mismatches = 0;
 
        System.out.println("\n=== " + period + " Stock Count ===");
        System.out.println("Date: " + TimeUtils.getDate());
        System.out.println("Time: " + TimeUtils.getTime());
        System.out.println("Outlet: " + currentOutlet.getOutletCode() + " - " + currentOutlet.getOutletName());
        System.out.println("Employee: " + currentUser.getName() + " (" + currentUser.getId() + ")\n");

        System.out.println("Employee: " + currentUser.getName() + " (" + currentUser.getId() + ")\n");

        for (Model s : stockList) {
            int counted = -1;
            while (counted < 0) {
                System.out.print("Model: " + s.getModelCode() + " – Counted: ");
                String input = sc.nextLine().trim();
                
                if (input.isEmpty()) {
                    System.out.println("\u001B[31mInvalid input. Please enter a number.\u001B[0m");
                    continue;
                }
                
                try {
                    counted = Integer.parseInt(input);
                    if (counted < 0) {
                        System.out.println("\u001B[31mInvalid input. Stock count cannot be negative.\u001B[0m");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("\u001B[31mInvalid input. Please enter a valid number.\u001B[0m");
                }
            }
            
            System.out.println("Store Record: " + s.getStock(currentOutlet.getOutletCode()));

            if (counted == s.getStock(currentOutlet.getOutletCode())) {
                System.out.println("\u001B[32mStock tally correct\u001B[0m.");
                tallyCorrect++;
            } else {
                System.out.println("\u001B[31m! Mismatch detected (" + Math.abs(counted - s.getStock(currentOutlet.getOutletCode())) + " unit difference)\u001B[0m");
                mismatches++;
            }

            totalChecked++;
        }

        System.out.println("\nTotal Models Checked: " + totalChecked);
        System.out.println("Tally \u001B[32mCorrect\u001B[0m: " + tallyCorrect);
        System.out.println("\u001B[31mMismatches\u001B[0m: " + mismatches);

        if (mismatches > 0) {
            System.out.println("\u001B[31mWarning: Please verify stock\u001B[0m.");
        }
        System.out.println(period + " stock count \u001B[32mcompleted\u001B[0m.");
        //sc.close();
    }
}