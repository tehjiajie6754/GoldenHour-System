package com.goldenhour.storage;

import java.io.*;
import java.util.*;

import com.goldenhour.categories.Attendance;
import com.goldenhour.categories.Employee;
import com.goldenhour.categories.Model;
import com.goldenhour.categories.Outlet;
import com.goldenhour.categories.Sales;

public class CSVHandler {


    private static final String EMPLOYEE_FILE   = "data/employee.csv";
    private static final String STOCK_FILE      = "data/model.csv";
    private static final String OUTLET_FILE     = "data/outlet.csv";
    private static final String ATTENDANCE_FILE = "data/attendance.csv";
    private static final String SALES_FILE      = "data/sales.csv";
    // private static final String EMPLOYEE_FILE = "C:\\FOP WIX1002\\FOP-Question-4\\data\\employee.csv";
    // private static final String STOCK_FILE = "C:\\FOP WIX1002\\FOP-Question-4\\data\\model.csv";
    // private static final String OUTLET_FILE = "C:\\FOP WIX1002\\FOP-Question-4\\data\\outlet.csv";
    // private static final String ATTENDANCE_FILE = "C:\\FOP WIX1002\\FOP-Question-4\\data\\attendance.csv";
    // private static final String SALES_FILE = "C:\\FOP WIX1002\\FOP-Question-4\\data\\sales.csv";

    public static List<Employee> readEmployees() {
        List<Employee> employees = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                employees.add(Employee.fromCSV(line));
            }
        } catch (IOException e) {
            System.out.println("Error reading employee file.");
        }
        return employees;
    }

    public static void writeEmployees(List<Employee> employees) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(EMPLOYEE_FILE))) {

            for (Employee emp : employees) {
                bw.write(emp.toCSV());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error writing employee file.");
        }
    }

    public static List<Attendance> readAttendance() {
        List<Attendance> attendance = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    attendance.add(Attendance.fromCSV(line));
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading attendance file.");
        }
        return attendance;
    }

    public static void writeAttendance(List<Attendance> attendance) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ATTENDANCE_FILE))) {
            bw.write("EmployeeID,EmployeeName,Date,ClockInTime,ClockOutTime,HoursWorked"); // header
            bw.newLine();
            for (Attendance att : attendance) {
                bw.write(att.toCSV());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error writing attendance file.");
        }
    }

    public static List<Model> readStock() {
        List<Model> stockList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(STOCK_FILE))) {
            String header = br.readLine(); // first line: Model,Price,C60,C61,...
            String[] outlets = header.split(",");
            // skip first 2 columns (Model, Price)
            String[] outletCodes = java.util.Arrays.copyOfRange(outlets, 2, outlets.length);

            String line;
            while ((line = br.readLine()) != null) {
                stockList.add(Model.fromCSV(line,outletCodes));
                //System.out.println("Loaded stock item from CSV: " + line);
            }
        } catch (IOException e) {
            System.out.println("Error reading stock file.");
        }
        return stockList;
    }

    public static void writeStock(List<Model> stockList) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(STOCK_FILE))) {

            String[] outletHeaders = {"C60", "C61", "C62", "C63", "C64", "C65", "C66", "C67", "C68", "C69","HQ"};
            
            bw.write("Model,Price," + String.join(",", outletHeaders));
            bw.newLine();

            for (Model s : stockList) {
                bw.write(s.toCSV(outletHeaders));
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error writing stock file.");
        }
    }

    public static List<Outlet> readOutlets() {
        List<Outlet> outlets = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(OUTLET_FILE))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                outlets.add(Outlet.fromCSV(line));
            }
        } catch (IOException e) {
            System.out.println("Error reading outlet file.");
        }
        return outlets;

    }

    
// ---------- SALES (read/write) ----------

    public static List<Sales> readSales() {
        List<Sales> sales = new ArrayList<>();
        File f = new File(SALES_FILE);
        if (!f.exists()) return sales;

        try (BufferedReader br = new BufferedReader(new FileReader(SALES_FILE))) {
            String first = br.readLine(); // header or first row
            // If the first line looks like a header, we already consumed it.
            // If it's data, we will process it below with 'line' loop as well.
            if (first != null && !first.trim().isEmpty()) {
                // Detect header by checking it starts with "date,time,"
                boolean isHeader = first.toLowerCase().startsWith("date,time,");
                if (!isHeader) {
                    // First line is actual data; parse it.
                    sales.add(Sales.fromCSV(first));
                }
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                sales.add(Sales.fromCSV(line));
            }
        } catch (IOException e) {
            System.out.println("Error reading sales file.");
        }
        return sales;
    }

    /**
     * Overwrites data/sales.csv with the provided list and writes the standard header first.
     * Use SalesService for appending per-transaction; this is for full rewrites (e.g., after edits).
     */
    public static void writeSales(List<Sales> allSales) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(SALES_FILE))) {
            // Write header
            bw.write("date,time,customerName,model,quantity,subtotal,transactionMethod,employee");
            bw.newLine();

            // Write records
            if (allSales != null) {
                for (Sales s : allSales) {
                    bw.write(s.toCSV());
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing sales file.");
        }

        
    }

    public static void writeOutlets(List<Outlet> outlets) {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(OUTLET_FILE))) {
            // Optional: Write header if your CSVs usually have them
            // bw.write("outlet_code,outlet_name");
            // bw.newLine();

            for (Outlet o : outlets) {
                // Format: code,name
                String line = o.getOutletCode() + "," + o.getOutletName();
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error writing outlets CSV: " + e.getMessage());
        }
    }

    /**
     * Appends a SINGLE sale to the CSV.
     * Called by SalesService after every transaction.
     */
    public static void appendSale(Sales sale) {
        File f = new File(SALES_FILE);
        boolean exists = f.exists();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
            // Write header if file is new
            if (!exists) {
                bw.write("date,time,customerName,model,quantity,subtotal,transactionMethod,employee");
                bw.newLine();
            }
            bw.write(sale.toCSV());
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error appending to sales file: " + e.getMessage());
        }
    }

}

