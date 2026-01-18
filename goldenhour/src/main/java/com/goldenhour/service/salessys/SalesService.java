package com.goldenhour.service.salessys;

import com.goldenhour.categories.*;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.service.loginregister.AuthService;
import com.goldenhour.storage.CSVHandler;
import com.goldenhour.storage.DatabaseHandler;
import com.goldenhour.storage.ReceiptHandler;
import com.goldenhour.utils.TimeUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SalesService {

    public void recordNewSale(Scanner sc, Employee employee) {
        if (employee == null) {
            System.out.println("Error: No employee logged in.");
            return;
        }

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

        if (todayAttendance == null){
            System.out.println("\u001B[31mError: You must sign attendance first before recording a sale.\u001B[0m");
            System.out.println("Please clock in at your outlet to continue.");
            return;
        }
        
        // Check if user has already clocked out
        if (todayAttendance.getClockOutTime() != null && !todayAttendance.getClockOutTime().isEmpty()) {
            System.out.println("\u001B[31mError: You have already clocked out today.\u001B[0m");
            System.out.println("Sales can only be recorded during your shift.");
            return;
        }

        System.out.println("\n=== Record New Sale ===");
        
        // 1. Shared details for the whole transaction
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String timeStr = now.format(DateTimeFormatter.ofPattern("hh:mm a"));
        
        System.out.println("Date: " + dateStr);
        System.out.println("Time: " + timeStr);
        System.out.print("Customer Name: ");
        String customer = sc.nextLine().trim();
        System.out.println("Item(s) Purchased: ");

        // 2. Validate Outlet (Once per transaction)
        String outletCode = "";
        while (true) {
            System.out.print("Enter outlet code (e.g., C60): ");
            String inputCode = sc.nextLine().trim();
            boolean exists = DataLoad.allOutlets.stream()
                    .anyMatch(o -> o.getOutletCode().equalsIgnoreCase(inputCode));
            
            if (exists) {
                outletCode = inputCode.toUpperCase();
                break;
            }
            System.out.println("Outlet not found.");
        }

        // --- MULTIPLE ITEM LOOP ---
        List<Sales> transactionItems = new ArrayList<>();
        // Tracks how much we are *planning* to buy in this session to prevent over-buying
        Map<String, Integer> tempStockTracker = new HashMap<>(); 
        
        boolean moreItems = true;
        double grandTotal = 0.0;

        do {
            System.out.println("\n--- Add Item ---");
            
            // A. Find Model
            Model selectedModel = null;
            while (selectedModel == null) {
                System.out.print("Enter Model: ");
                String inputModel = sc.nextLine().trim();
                
                selectedModel = DataLoad.allModels.stream()
                        .filter(m -> m.getModelCode().equalsIgnoreCase(inputModel) 
                                  || m.getModelCode().equalsIgnoreCase(inputModel))
                        .findFirst()
                        .orElse(null);
                
                if (selectedModel == null) System.out.println("Model not found.");
            }

            // B. Calculate Available Stock (considering items already in cart)
            int currentRealStock = selectedModel.getStock(outletCode); // FIX: Use outlet stock
            int alreadyInCart = tempStockTracker.getOrDefault(selectedModel.getModelCode(), 0);
            int availableForThisEntry = currentRealStock - alreadyInCart;

            if (availableForThisEntry <= 0) {
                System.out.println("No stock left for this model in this transaction!");
                continue; // Skip to next loop iteration
            }

            // C. Enter Quantity
            int qty = 0;
            while (true) {
                System.out.print("Enter Quantity (Available: " + availableForThisEntry + "): ");
                try {
                    String input = sc.nextLine().trim();
                    qty = Integer.parseInt(input);
                    
                    if (qty <= 0) {
                        System.out.println("Quantity must be positive.");
                    } else if (qty > availableForThisEntry) {
                        System.out.println("Insufficient stock!");
                    } else {
                        break;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number.");
                }
            }

            // D. Price & Stage Item
            double unitPrice = selectedModel.getPrice();
            double lineSubtotal = unitPrice * qty;
            grandTotal += lineSubtotal;

            // Create temporary Sales object (Not saved yet!)
            Sales lineItem = new Sales(dateStr, timeStr, customer, selectedModel.getModelCode(), qty, lineSubtotal, "PENDING", employee.getName(),outletCode,employee.getId());
            transactionItems.add(lineItem);

            // Update tracker
            tempStockTracker.put(selectedModel.getModelCode(), alreadyInCart + qty);
            
            System.out.println("Item added! Current Total: RM" + grandTotal);

            // E. Ask for more
            System.out.print("Add another item? (Y/N): ");
            String choice = sc.nextLine().trim();
            if (choice.equalsIgnoreCase("N")) {
                moreItems = false;
            }

        } while (moreItems);

        // --- FINALIZE TRANSACTION ---
        if (transactionItems.isEmpty()) {
            System.out.println("Transaction cancelled (No items added).");
            return;
        }

        System.out.println("\nGrand Total: RM" + grandTotal);
        System.out.print("Enter transaction method (Cash/Card/E-Wallet): ");
        String method = sc.nextLine().trim();

        // Check Confirmation
        System.out.print("Confirm transaction? (Y/N): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Transaction cancelled.");
            return;
        }

        // --- EXECUTE UPDATES ---
        StringBuilder receiptBody = new StringBuilder();

        for (Sales item : transactionItems) {
            // 1. Update Payment Method (was PENDING)
            item.setTransactionMethod(method);

            // 2. Deduct Stock
            // We must find the model object again to update the static DataLoad list
            Model m = DataLoad.allModels.stream()
                    .filter(mod -> mod.getModelCode().equalsIgnoreCase(item.getModel()))
                    .findFirst().orElse(null);
            
            if (m != null) {
                int current = m.getStock(outletCode);
                int newStock = current - item.getQuantity();
                m.setStock(outletCode, newStock);
                
                DatabaseHandler.updateStock(m.getModelCode(), outletCode, newStock); // ✅ DB WRITE
            }

            // 3. Save to Memory & File
            DataLoad.allSales.add(item);
            DatabaseHandler.saveSale(item);
            CSVHandler.appendSale(item);

            // 4. Build Receipt Line
            receiptBody.append(String.format("%-15s x%2d  RM%8.2f\n", item.getModel(), item.getQuantity(), item.getSubtotal()));
        }

        // 5. Update Stock File (Once after all deductions)
        CSVHandler.writeStock(DataLoad.allModels);

        // 6. Generate Full Receipt String
        String finalReceipt = 
                "=== Sales Receipt ===\n" +
                "Date: " + dateStr + "\n" +
                "Time: " + timeStr + "\n" +
                "Outlet: " + outletCode + "\n" +
                "Customer: " + customer + "\n" +
                "----------------------------------\n" +
                "ITEMS PURCHASED:\n" +
                receiptBody.toString() +
                "----------------------------------\n" +
                "Total Items: " + transactionItems.size() + "\n" +
                "Grand Total: RM " + grandTotal + "\n" +
                "Method: " + method + "\n" +
                "Employee: " + employee.getName() + "\n";

        // 7. Save Receipt
        String fileName = ReceiptHandler.appendSalesReceipt(finalReceipt, "sales");

        System.out.println("\nTransaction \u001B[32msuccessful\u001B[0m.");
        System.out.println("Sale recorded \u001B[32msuccessfully\u001B[0m.");
        System.out.println("Model quantities updated \u001B[32msuccessfully\u001B[0m.");
        System.out.println("Receipt generated: " + fileName);
    }
}