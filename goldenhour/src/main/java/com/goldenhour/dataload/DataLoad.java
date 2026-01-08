package com.goldenhour.dataload;

import com.goldenhour.categories.*;
import com.goldenhour.storage.DatabaseHandler;
import java.util.ArrayList;
import java.util.List;

public class DataLoad {

    public static List<Employee> allEmployees = new ArrayList<>();
    public static List<Outlet> allOutlets = new ArrayList<>();
    public static List<Model> allModels = new ArrayList<>();
    public static List<Sales> allSales = new ArrayList<>();
    public static List<Attendance> allAttendance = new ArrayList<>();

    public static void loadAllData() {
        System.out.println("\n\u001B[32mLoading data from SQLite\u001B[0m...");
        
        allEmployees = DatabaseHandler.fetchAllEmployees();
        allOutlets = DatabaseHandler.fetchAllOutlets();
        allModels = DatabaseHandler.fetchAllModelsWithStock();
        allSales = DatabaseHandler.fetchAllSales();
        allAttendance = DatabaseHandler.fetchAllAttendance();

        if (allEmployees.isEmpty()) {
            System.out.println("\u001B[33mWarning: Employee data lists are empty. Please check the database.\u001B[0m");
        } else if (allOutlets.isEmpty()) {
            System.out.println("\u001B[33mWarning: Outlet data lists are empty. Please check the database.\u001B[0m");
        } else if (allModels.isEmpty()) {
            System.out.println("\u001B[33mWarning: Model data lists are empty. Please check the database.\u001B[0m");
        } else if (allSales.isEmpty()) {
            System.out.println("\u001B[33mWarning: Sales data lists are empty. Please check the database.\u001B[0m");
        } else if (allAttendance.isEmpty()) {
            System.out.println("\u001B[33mWarning: Attendance data lists are empty. Please check the database.\u001B[0m");
        } else {
            System.out.println("Data loaded \u001B[32msuccessfully\u001B[0m!");
        }
        
        
    }
}