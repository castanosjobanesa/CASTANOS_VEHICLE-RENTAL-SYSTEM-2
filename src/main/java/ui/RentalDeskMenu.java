package com.scc.ui;

import com.scc.dao.CustomerDao;
import com.scc.dao.RentalDao;
import com.scc.dao.VehicleDao;
import com.scc.model.Rental;
import com.scc.model.Vehicle;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class RentalDeskMenu {
    private final RentalDao rentalDao = new RentalDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final Scanner scanner;
    private final User currentAgent;

    public RentalDeskMenu(Scanner scanner, User currentAgent) {
        this.scanner = scanner;
        this.currentAgent = currentAgent;
    }

    public void start() {
        while (true) {
            System.out.println("\n=======================================================");
            System.out.println("============== RENTAL DESK AGENT MENU =================");
            System.out.println("=======================================================");
            System.out.println("1. Register Customer Profile (CREATE)");
            System.out.println("2. Process Vehicle Rental (CREATE)");
            System.out.println("3. Search & View Available Vehicles (READ Filter)");
            System.out.println("4. Look up Rentals by Customer Name or ID (READ Search)");
            System.out.println("5. Process Vehicle Return (UPDATE Status)");
            System.out.println("6. Cancel Rental Transaction (UPDATE Status)");
            System.out.println("7. Modify Customer Contact Details (UPDATE Profile)");
            System.out.println("8. Logout");
            System.out.print("Choose an option: ");

            int choice = readIntInput("");
            switch (choice) {
                case 1 -> handleRegisterCustomer();
                case 2 -> handleProcessRental();
                case 3 -> handleSearchVehicles();
                case 4 -> handleSearchRentals();
                case 5 -> handleProcessReturn();
                case 6 -> handleCancelRental();
                case 7 -> handleUpdateCustomer();
                case 8 -> {
                    System.out.println("Logging out from Rental Desk session.");
                    return;
                }
                default -> System.out.println("Invalid choice. Please choose between 1 and 8.");
            }
        }
    }

    private int readIntInput(String prompt) {
        while (true) {
            if (!prompt.isEmpty()) {
                System.out.print(prompt);
            }
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }

    private void handleRegisterCustomer() {
        System.out.println("\n--- 1. CUSTOMER REGISTRATION ---");
        System.out.print("Enter Full Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter Driver's License Number: ");
        String license = scanner.nextLine().trim();
        System.out.print("Enter Contact Number: ");
        String contact = scanner.nextLine().trim();
        System.out.print("Enter Address: ");
        String address = scanner.nextLine().trim();

        boolean success = customerDao.registerCustomer(currentAgent.userId(), name, license, contact, address);
        if (success) {
            System.out.println("Customer profile successfully registered and logged!");
        } else {
            System.out.println("Failed to register customer (License number might already exist).");
        }
    }

    private void handleProcessRental() {
        System.out.println("\n--- 2. VEHICLE RENTAL ---");
        System.out.print("Enter Customer Name: ");
        String customerName = scanner.nextLine().trim();

        System.out.print("Enter Vehicle Details (e.g., Toyota Vios - Plate ABC1234): ");
        String vehicleDetails = scanner.nextLine().trim();

        System.out.print("Enter Rental Type (Daily / Weekly): ");
        String rentalType = scanner.nextLine().trim();

        System.out.print("Enter Total Amount (P): ");
        double amount = 0;
        while (true) {
            try {
                amount = Double.parseDouble(scanner.nextLine().trim());
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a valid number.");
            }
        }

        boolean success = rentalDao.createRentalTransaction(currentAgent.userId(), customerName, vehicleDetails, rentalType, amount);
        if (success) {
            System.out.println("Vehicle rental transaction successfully recorded and logged!");
        } else {
            System.out.println("Failed to process vehicle rental.");
        }
    }

    private void handleSearchVehicles() {
        System.out.println("\n--- 3. SEARCH VEHICLES (Filter by Type or Status) ---");
        System.out.print("Enter Vehicle Category filter (leave blank for all): ");
        String typeFilter = scanner.nextLine().trim();
        System.out.print("Enter Status filter [AVAILABLE / RENTED / MAINTENANCE] (leave blank for all): ");
        String statusFilter = scanner.nextLine().trim();

        List<Vehicle> vehicles = vehicleDao.getFilteredVehicles(typeFilter, statusFilter);
        System.out.println("\n==================================================================================");
        System.out.println("================================ VEHICLE INVENTORY ===============================");
        System.out.println("==================================================================================");
        System.out.printf("%-5s | %-15s | %-20s | %-15s | %-10s | %-12s%n", "ID", "Plate No.", "Model", "Category", "Rate", "Status");
        System.out.println("----------------------------------------------------------------------------------");
        if (vehicles.isEmpty()) {
            System.out.println("No matching vehicles found.");
        } else {
            for (Vehicle v : vehicles) {
                System.out.printf("%-5d | %-15s | %-20s | %-15s | P%-9.2f | %-12s%n",
                        v.vehicleId(), v.plateNumber(), v.model(), v.category(), v.dailyRate(), v.status());
            }
        }
        System.out.println("--------------------------------------------------------------------------------==");
    }

    private void handleSearchRentals() {
        System.out.println("\n--- 4. SEARCH RENTALS (By Customer Name or Rental ID) ---");
        System.out.print("Enter Customer Name or Rental ID keyword: ");
        String keyword = scanner.nextLine().trim();

        List<Rental> rentals = rentalDao.searchRentals(keyword);
        displayRentalTable(rentals);
    }

    private void handleProcessReturn() {
        System.out.println("\n--- 5. VEHICLE RETURN ---");
        displayRentalTable(rentalDao.getAllRentals());
        int rentalId = readIntInput("\nEnter Rental ID to process return: ");
        if (rentalId == 0) return;

        boolean success = rentalDao.updateRentalStatus(currentAgent.userId(), rentalId, "COMPLETED");
        if (success) {
            System.out.println("Vehicle return completed successfully for rental #" + rentalId);
        } else {
            System.out.println("Failed to process vehicle return.");
        }
    }

    private void handleCancelRental() {
        System.out.println("\n--- 6. RENTAL CANCELLATION ---");
        displayRentalTable(rentalDao.getAllRentals());
        int rentalId = readIntInput("\nEnter Rental ID to cancel: ");
        if (rentalId == 0) return;

        boolean success = rentalDao.updateRentalStatus(currentAgent.userId(), rentalId, "CANCELLED");
        if (success) {
            System.out.println("Rental #" + rentalId + " successfully cancelled and logged.");
        } else {
            System.out.println("Failed to cancel rental transaction.");
        }
    }

    private void handleUpdateCustomer() {
        System.out.println("\n--- 7. MODIFY CUSTOMER CONTACT DETAILS ---");
        int customerId = readIntInput("Enter Customer ID to update: ");
        System.out.print("Enter New Contact Number: ");
        String newContact = scanner.nextLine().trim();
        System.out.print("Enter New Address: ");
        String newAddress = scanner.nextLine().trim();

        boolean success = customerDao.updateCustomerContact(currentAgent.userId(), customerId, newContact, newAddress);
        if (success) {
            System.out.println("Customer contact details updated successfully.");
        } else {
            System.out.println("Failed to update customer details.");
        }
    }

    private void displayRentalTable(List<Rental> list) {
        System.out.println("\n=================================================================================================================");
        System.out.println("========================================== RENTAL RECORDS =======================================================");
        System.out.println("=================================================================================================================");
        System.out.printf("%-5s | %-8s | %-20s | %-25s | %-12s | %-10s | %-10s | %-20s%n", "ID", "User ID", "Customer", "Vehicle", "Type", "Amount", "Status", "Timestamp");
        System.out.println("-----------------------------------------------------------------------------------------------------------------");
        if (list.isEmpty()) {
            System.out.println("No rental records found.");
        } else {
            for (Rental t : list) {
                System.out.printf("%-5s | %-8s | %-20s | %-25s | %-12s | P%-9.2f | %-10s | %-20s%n",
                        t.rentalId(), t.userId(), t.customerName(), t.vehicleDetails(), t.rentalType(), t.totalAmount(), t.status(), t.timestamp());
            }
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------");
    }
}