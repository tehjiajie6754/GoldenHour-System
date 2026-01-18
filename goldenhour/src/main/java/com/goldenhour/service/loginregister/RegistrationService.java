package com.goldenhour.service.loginregister;

import com.goldenhour.categories.Employee;
import com.goldenhour.storage.CSVHandler;
import com.goldenhour.storage.DatabaseHandler;
import com.goldenhour.dataload.DataLoad;

import java.util.*;

public class RegistrationService {

    public static void registerEmployee(String id, String name, String role, String password) {

        List<Employee> employees = DataLoad.allEmployees;

        // Validate role
        if (!role.equals("Full-time") && !role.equals("Part-time") && !role.equals("Manager")) {
            System.out.println("\u001B[31mError: Invalid role. Role must be 'Full-time', 'Part-time', or 'Manager'.\u001B[0m");
            return;
        }

        for (Employee e : employees) {
            if (e.getId().equals(id)) {
                System.out.println("Error: Employee ID already exists!");
                return;
            }
        }

        Employee newEmployee = new Employee(id,name,role,password);
        employees.add(newEmployee);
        // ✅ 2. Save to Database
        DatabaseHandler.saveEmployee(newEmployee);
        CSVHandler.writeEmployees(employees);
        System.out.println("\nEmployee \u001B[32msuccessfully registered!\u001B[0m");
    }
}
