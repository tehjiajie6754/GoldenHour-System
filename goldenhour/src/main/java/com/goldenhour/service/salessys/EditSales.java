package com.goldenhour.service.salessys;

import com.goldenhour.categories.Sales;
import com.goldenhour.categories.Model;
import com.goldenhour.dataload.DataLoad;
import com.goldenhour.storage.CSVHandler;
import com.goldenhour.storage.DatabaseHandler;

import java.util.Scanner;

public class EditSales {

    /**
     * Helper method to get the unit price of a model.
     */
    private double getModelPrice(String modelCode) {
        return DataLoad.allModels.stream()
                .filter(m -> m.getModelCode().equalsIgnoreCase(modelCode))
                .map(Model::getPrice)
                .findFirst()
                .orElse(0.0);
    }

    /**
     * Helper method to recalculate total price based on quantity and model price.
     */
    private void recalculateTotal(Sales sale) {
        double unitPrice = getModelPrice(sale.getModel());
        double newTotal = unitPrice * sale.getQuantity();
        sale.setSubtotal(newTotal);
    }

    public void editSales(Scanner sc) {

        System.out.println("\n=== Edit Sales Record ===");
        
        // 1. Get Identification Details
        System.out.print("Enter transaction date (yyyy-MM-dd): ");
        String date = sc.nextLine().trim();
        System.out.print("Enter customer name: ");
        String customer = sc.nextLine().trim();
        System.out.print("Enter model name: ");
        String model = sc.nextLine().trim();

        // 2. Find the object in memory (DataLoad)
        Sales targetSale = DataLoad.allSales.stream()
                .filter(s -> s.getDate().equals(date) 
                          && s.getCustomerName().equalsIgnoreCase(customer)
                          && s.getModel().equalsIgnoreCase(model))
                .findFirst()
                .orElse(null);

        if (targetSale == null) {
            System.out.println("Sale record not found in memory.");
            return;
        }

        System.out.println("\n=== Sale Record Found ===");
        System.out.println(targetSale); // Uses Sales.toString()

        // 3. Menu for Editing
        System.out.println("\nSelect number to edit:");
        System.out.println("1. Customer Name");
        System.out.println("2. Model");
        System.out.println("3. Quantity");
        System.out.println("4. Total Price (subtotal)");
        System.out.println("5. Transaction Method");
        System.out.println("6. Cancel");
        System.out.print("> ");

        String choice = sc.nextLine().trim();
        boolean changed = false;

        System.out.println();

        try {
            switch (choice) {
                case "1":
                    System.out.print("New Customer Name: ");
                    targetSale.setCustomerName(sc.nextLine().trim());
                    changed = true;
                    break;
                case "2":
                    System.out.print("New Model: ");
                    String newModel = sc.nextLine().trim();
                    double newPrice = getModelPrice(newModel);
                    if (newPrice == 0.0) {
                        System.out.println("Model not found. Changes cancelled.");
                        return;
                    }
                    targetSale.setModel(newModel);
                    // Automatically recalculate total with new model's price
                    recalculateTotal(targetSale);
                    System.out.println("Model updated. Total price recalculated: RM" + String.format("%.2f", targetSale.getSubtotal()));
                    changed = true;
                    break;
                case "3":
                    System.out.print("New Quantity: ");
                    int newQty = Integer.parseInt(sc.nextLine().trim());
                    if (newQty <= 0) {
                        System.out.println("Quantity must be positive. Changes cancelled.");
                        return;
                    }
                    targetSale.setQuantity(newQty);
                    // Automatically recalculate total with new quantity
                    recalculateTotal(targetSale);
                    System.out.println("Quantity updated. Total price recalculated: RM" + String.format("%.2f", targetSale.getSubtotal()));
                    changed = true;
                    break;
                case "4":
                    System.out.print("New Total Price (RM): ");
                    targetSale.setSubtotal(Double.parseDouble(sc.nextLine().trim()));
                    changed = true;
                    break;
                case "5":
                    System.out.print("New Transaction Method: ");
                    targetSale.setTransactionMethod(sc.nextLine().trim());
                    changed = true;
                    break;
                case "6":
                    System.out.println("Edit cancelled.");
                    return;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
            return;
        }

        // 4. Save Changes (Rewrite the whole file)
        if (changed) {
            // Since we modified the object inside DataLoad.allSales, 
            // the memory is ALREADY updated. We just need to save to disk.
            DatabaseHandler.updateSale(targetSale, date, customer, model);
            CSVHandler.writeSales(DataLoad.allSales);
            System.out.println("Sale record updated \u001B[32msuccessfully!\u001B[0m");
        }
    }
}