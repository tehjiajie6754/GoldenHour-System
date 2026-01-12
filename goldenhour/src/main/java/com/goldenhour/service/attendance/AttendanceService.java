package com.goldenhour.service.attendance;

import com.goldenhour.categories.Attendance;
import com.goldenhour.categories.Employee;
import com.goldenhour.categories.Outlet;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.service.loginregister.AuthService;
import com.goldenhour.storage.CSVHandler;
import com.goldenhour.storage.DatabaseHandler;
import com.goldenhour.utils.TimeUtils;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class AttendanceService {
    private static Scanner sc = new Scanner(System.in);

    public static void clockIn() {
        Employee emp = AuthService.getCurrentUser();
        if (emp == null) {
            System.out.println("No user logged in.");
            return;
        }

        String today = TimeUtils.getDate();
        String clockInTime = TimeUtils.getTime();
        List<Attendance> records = DataLoad.allAttendance;

        // Check if employee already clocked in today
        for (Attendance a : records) {
            if (a.getEmployeeId().equals(emp.getId()) && a.getDate().equals(today) 
                    && a.getClockInTime() != null) {
                System.out.println("Error: You have already clocked in today at " + a.getClockInTime());
                return;
            }
        }

        // Select outlet
        String outletCode = selectOutlet();
        if (outletCode == null) {
            System.out.println("Outlet selection cancelled.");
            return;
        }

        String outletName = getOutletName(outletCode);

        Attendance newRecord = new Attendance(emp.getId(), emp.getName(), today, clockInTime, outletCode);
        records.add(newRecord);
        DatabaseHandler.saveAttendance(newRecord);
        CSVHandler.writeAttendance(records);

        // Display output as per requirement
        System.out.println("\n=== Attendance Clock In ===");
        System.out.println("Employee ID: " + emp.getId());
        System.out.println("Name: " + emp.getName());
        System.out.println("Outlet: " + outletCode + " (" + outletName + ")");
        System.out.println();
        System.out.println("Clock In \u001B[32mSuccessful!\u001B[0m"); // Green color
        System.out.println("Date: " + today);
        System.out.println("Time: " + clockInTime);
    }

    public static void clockOut() {
        Employee emp = AuthService.getCurrentUser();
        if (emp == null) {
            System.out.println("No user logged in.");
            return;
        }

        String today = TimeUtils.getDate();
        String clockOutTime = TimeUtils.getTime();
        List<Attendance> records = DataLoad.allAttendance;

        Attendance todayRecord = null;
        for (Attendance a : records) {
            if (a.getEmployeeId().equals(emp.getId()) && a.getDate().equals(today)) {
                todayRecord = a;
                break;
            }
        }

        if (todayRecord == null) {
            System.out.println("Error: No clock-in record found for today. Please clock in first.");
            return;
        }

        if (todayRecord.getClockOutTime() != null) {
            System.out.println("Error: You have already clocked out today at " + todayRecord.getClockOutTime());
            return;
        }

        // Calculate hours worked
        double hoursWorked = calculateHours(todayRecord.getClockInTime(), clockOutTime);
        
        todayRecord.setClockOutTime(clockOutTime);
        todayRecord.setHoursWorked(hoursWorked);

        DatabaseHandler.updateAttendance(todayRecord);
        
        CSVHandler.writeAttendance(records);

        // Use outlet from clock-in record
        String outletCode = todayRecord.getOutletCode() != null ? todayRecord.getOutletCode() : "C60";
        String outletName = getOutletName(outletCode);

        // Display output as per requirement
        System.out.println("\n=== Attendance Clock Out ===");
        System.out.println("Employee ID: " + emp.getId());
        System.out.println("Name: " + emp.getName());
        System.out.println("Outlet: " + outletCode + " (" + outletName + ")");
        System.out.println();
        System.out.println("Clock Out \u001B[32mSuccessful!\u001B[0m"); // Green color
        System.out.println("Date: " + today);
        System.out.println("Time: " + clockOutTime);
        System.out.printf("Total Hours Worked: %.1f hours\n", hoursWorked);
    }

    private static double calculateHours(String clockIn, String clockOut) {
        try {
            //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
            LocalDateTime inTime = LocalDateTime.parse("2025-01-01 " + clockIn, 
                                   DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
            LocalDateTime outTime = LocalDateTime.parse("2025-01-01 " + clockOut, 
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
            
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(inTime, outTime);
            return minutes / 60.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String selectOutlet() {
        List<Outlet> outlets = DataLoad.allOutlets;
        
        System.out.println("\nSelect Outlet:");
        for (int i = 0; i < outlets.size(); i++) {
            System.out.println((i + 1) + ". " + outlets.get(i).getOutletCode() + 
                             " - " + outlets.get(i).getOutletName());
        }
        System.out.print("Enter outlet code/name: ");
        
        String input = sc.nextLine().trim();
        
        // Try as number first
        try {
            int choice = Integer.parseInt(input);
            if (choice > 0 && choice <= outlets.size()) {
                return outlets.get(choice - 1).getOutletCode();
            }
        } catch (NumberFormatException e) {

        }
        return null;
    }

    private static String getOutletName(String outletCode) {
        for (Outlet o : DataLoad.allOutlets) {
            if (o.getOutletCode().equals(outletCode)) {
                return o.getOutletName();
            }
        }
        return "Unknown";
    }

    public static void viewAttendance(String employeeId) {
        List<Attendance> records = DataLoad.allAttendance;
        boolean found = false;

        System.out.println("\n=== Attendance Records for " + employeeId + " ===");
        for (Attendance a : records) {
            if (a.getEmployeeId().equals(employeeId)) {
                System.out.println(a);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No attendance records found.");
        }
    }
}
