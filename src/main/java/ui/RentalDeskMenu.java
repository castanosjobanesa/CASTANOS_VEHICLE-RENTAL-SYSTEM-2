package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.CustomerDao;
import com.scc.dao.RentalDao;
import com.scc.dao.VehicleDao;
import com.scc.model.Rental;
import com.scc.model.User;
import com.scc.model.Vehicle;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class RentalDeskMenu {
    private final RentalDao rentalDao = new RentalDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
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
            System.out.println("1. Register Customer Profile");
            System.out.println("2. Process Vehicle Rental with Auto-Pricing");
            System.out.println("3. Search & View Available Vehicles");
            System.out.println("4. Look up Rentals by Customer Name or ID ");
            System.out.println("5. Process Vehicle Return & Update Vehicle Status ");
            System.out.println("6. Extend Rental Duration & Return Date ");
            System.out.println("7. Cancel / Void Rental Transaction ");
            System.out.println("8. Modify Customer Contact Details ");
            System.out.println("9. Transaction Summary Report ");
            System.out.println("10. Logout");
            System.out.print("Choose an option: ");

            int choice = readIntInput("");
            switch (choice) {
                case 1 -> handleRegisterCustomer();
                case 2 -> handleProcessRental();
                case 3 -> handleSearchVehicles();
                case 4 -> handleSearchRentals();
                case 5 -> handleProcessReturn();
                case 6 -> handleExtendRental();
                case 7 -> handleCancelRental();
                case 8 -> handleUpdateCustomer();
                case 9 -> handleTransactionSummary();
                case 10 -> {
                    System.out.println("Logging out from Rental Desk session.");
                    return;
                }
                default -> System.out.println("Invalid choice. Please choose between 1 and 10.");
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
            activityLogDao.logAction(currentAgent.userId(), "CUSTOMER_CREATE",
                    "Registered customer '" + name + "' (License: " + license + ").");
            System.out.println("Customer profile successfully registered and logged!");
        } else {
            System.out.println("Failed to register customer (License number might already exist).");
        }
    }

    private void handleProcessRental() {
        System.out.println("\n--- 2. VEHICLE RENTAL TRANSACTION ---");
        List<Vehicle> availableVehicles = vehicleDao.getFilteredVehicles("", "AVAILABLE");
        if (availableVehicles.isEmpty()) {
            System.out.println("No AVAILABLE vehicles found in inventory.");
            return;
        }

        displayVehicleTable(availableVehicles);
        int selectedVehicleId = readIntInput("Select Vehicle ID to rent (or 0 to cancel): ");
        if (selectedVehicleId == 0) return;

        Vehicle selectedVehicle = availableVehicles.stream()
                .filter(v -> v.vehicleId() == selectedVehicleId)
                .findFirst()
                .orElse(null);

        if (selectedVehicle == null) {
            System.out.println("Invalid Vehicle ID or vehicle is not currently AVAILABLE.");
            return;
        }

        System.out.print("Enter Customer Name: ");
        String customerName = scanner.nextLine().trim();

        int days;
        while (true) {
            days = readIntInput("Enter Rental Duration in Days: ");
            if (days > 0) break;
            System.out.println("Duration must be at least 1 day.");
        }

        String returnDate = LocalDate.now().plusDays(days).toString();
        String rentalType = (days >= 7 ? "WEEKLY" : "DAILY") + " (" + days + "d, Return: " + returnDate + ")";
        double totalAmount = selectedVehicle.dailyRate() * days;
        String vehicleDetails = "[" + selectedVehicle.vehicleId() + "] " + selectedVehicle.model() + " - " + selectedVehicle.plateNumber();

        System.out.printf("Calculated Total: P%.2f (P%.2f/day x %d days) | Expected Return Date: %s%n",
                totalAmount, selectedVehicle.dailyRate(), days, returnDate);
        System.out.print("Confirm rental transaction? (Y/N): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Rental transaction cancelled before creation.");
            return;
        }

        boolean success = rentalDao.createRentalTransaction(
                currentAgent.userId(),
                customerName,
                vehicleDetails,
                rentalType,
                totalAmount
        );

        if (success) {
            vehicleDao.updateVehicleStatus(currentAgent.userId(), selectedVehicle.vehicleId(), "RENTED");
            activityLogDao.logAction(currentAgent.userId(), "RENTAL_CREATE",
                    "Created rental for '" + customerName + "' on " + vehicleDetails +
                            " for " + days + " day(s), Return: " + returnDate + ", Total: P" + String.format("%.2f", totalAmount));
            System.out.println("Vehicle rental transaction recorded and vehicle marked as RENTED!");
        } else {
            System.out.println("Failed to process vehicle rental.");
        }
    }

    private void handleSearchVehicles() {
        System.out.println("\n--- 3. SEARCH VEHICLES (Filter by Category or Status) ---");
        System.out.print("Enter Vehicle Category filter (leave blank for all): ");
        String typeFilter = scanner.nextLine().trim();
        System.out.print("Enter Status filter [AVAILABLE / RENTED / UNDER SERVICE / MAINTENANCE] (leave blank for all): ");
        String statusFilter = scanner.nextLine().trim();

        List<Vehicle> vehicles = vehicleDao.getFilteredVehicles(typeFilter, statusFilter);
        displayVehicleTable(vehicles);
    }

    private void handleSearchRentals() {
        System.out.println("\n--- 4. SEARCH RENTALS (By Customer Name or Rental ID) ---");
        System.out.print("Enter Customer Name or Rental ID keyword: ");
        String keyword = scanner.nextLine().trim();

        List<Rental> rentals = rentalDao.searchRentals(keyword);
        displayRentalTable(rentals);
    }

    private void handleProcessReturn() {
        System.out.println("\n--- 5. VEHICLE RETURN & STATUS UPDATE ---");
        List<Rental> rentals = rentalDao.getAllRentals();
        displayRentalTable(rentals);
        int rentalId = readIntInput("\nEnter Rental ID to process return (or 0 to cancel): ");
        if (rentalId == 0) return;

        Rental target = rentals.stream()
                .filter(r -> r.rentalId() == rentalId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println("Rental #" + rentalId + " not found.");
            return;
        }

        System.out.println("Select updated vehicle status:");
        System.out.println("1. AVAILABLE");
        System.out.println("2. UNDER SERVICE");
        int statusChoice = readIntInput("Choose (1-2): ");
        String newVehicleStatus = (statusChoice == 2) ? "UNDER SERVICE" : "AVAILABLE";

        boolean success = rentalDao.updateRentalStatus(currentAgent.userId(), rentalId, "COMPLETED");
        if (success) {
            int vehicleId = extractVehicleId(target.vehicleDetails());
            if (vehicleId > 0) {
                vehicleDao.updateVehicleStatus(currentAgent.userId(), vehicleId, newVehicleStatus);
            }
            activityLogDao.logAction(currentAgent.userId(), "RENTAL_RETURN",
                    "Completed rental #" + rentalId + "; vehicle status set to " + newVehicleStatus + ".");
            System.out.println("Vehicle return completed successfully for rental #" + rentalId);
        } else {
            System.out.println("Failed to process vehicle return.");
        }
    }

    private void handleExtendRental() {
        System.out.println("\n--- 6. EXTEND RENTAL DURATION ---");
        List<Rental> rentals = rentalDao.getAllRentals();
        displayRentalTable(rentals);
        int rentalId = readIntInput("\nEnter Rental ID to extend (or 0 to cancel): ");
        if (rentalId == 0) return;

        Rental target = rentals.stream()
                .filter(r -> r.rentalId() == rentalId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println("Rental #" + rentalId + " not found.");
            return;
        }

        int extraDays;
        while (true) {
            extraDays = readIntInput("Enter additional days to add: ");
            if (extraDays > 0) break;
            System.out.println("Additional days must be greater than 0.");
        }

        int vehicleId = extractVehicleId(target.vehicleDetails());
        double dailyRate = 0.0;
        if (vehicleId > 0) {
            Vehicle v = vehicleDao.getFilteredVehicles("", "").stream()
                    .filter(item -> item.vehicleId() == vehicleId)
                    .findFirst()
                    .orElse(null);
            if (v != null) {
                dailyRate = v.dailyRate();
            }
        }

        if (dailyRate <= 0.0) {
            System.out.print("Enter Daily Rate (P) for extension calculation: ");
            while (true) {
                try {
                    dailyRate = Double.parseDouble(scanner.nextLine().trim());
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid rate. Please enter a valid number.");
                }
            }
        }

        double addedAmount = dailyRate * extraDays;
        double updatedTotal = target.totalAmount() + addedAmount;
        String newReturnDate = LocalDate.now().plusDays(extraDays).toString();

        boolean success = rentalDao.extendRentalDuration(currentAgent.userId(), rentalId, extraDays, newReturnDate, updatedTotal);
        if (success) {
            activityLogDao.logAction(currentAgent.userId(), "RENTAL_EXTEND",
                    "Extended rental #" + rentalId + " by +" + extraDays + " day(s); new return date: " +
                            newReturnDate + ", new total: P" + String.format("%.2f", updatedTotal));
            System.out.printf("Rental #%d extended by %d day(s). New Return Date: %s | New Total: P%.2f%n",
                    rentalId, extraDays, newReturnDate, updatedTotal);
        } else {
            System.out.println("Failed to extend rental duration.");
        }
    }

    private void handleCancelRental() {
        System.out.println("\n--- 7. CANCEL / VOID RENTAL TRANSACTION ---");
        List<Rental> rentals = rentalDao.getAllRentals();
        displayRentalTable(rentals);
        int rentalId = readIntInput("\nEnter Rental ID to cancel/void (or 0 to cancel): ");
        if (rentalId == 0) return;

        Rental target = rentals.stream()
                .filter(r -> r.rentalId() == rentalId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println("Rental #" + rentalId + " not found.");
            return;
        }

        boolean success = rentalDao.cancelOrVoidRental(currentAgent.userId(), rentalId);
        if (success) {
            int vehicleId = extractVehicleId(target.vehicleDetails());
            if (vehicleId > 0) {
                vehicleDao.updateVehicleStatus(currentAgent.userId(), vehicleId, "AVAILABLE");
            }
            activityLogDao.logAction(currentAgent.userId(), "RENTAL_CANCEL",
                    "Cancelled/voided rental #" + rentalId + " and released vehicle.");
            System.out.println("Rental #" + rentalId + " successfully cancelled/voided and logged.");
        } else {
            System.out.println("Failed to cancel rental transaction.");
        }
    }

    private void handleUpdateCustomer() {
        System.out.println("\n--- 8. MODIFY CUSTOMER CONTACT DETAILS ---");
        int customerId = readIntInput("Enter Customer ID to update: ");
        System.out.print("Enter New Contact Number: ");
        String newContact = scanner.nextLine().trim();
        System.out.print("Enter New Address: ");
        String newAddress = scanner.nextLine().trim();

        boolean success = customerDao.updateCustomerContact(currentAgent.userId(), customerId, newContact, newAddress);
        if (success) {
            activityLogDao.logAction(currentAgent.userId(), "CUSTOMER_UPDATE",
                    "Updated contact details for Customer ID #" + customerId + ".");
            System.out.println("Customer contact details updated successfully.");
        } else {
            System.out.println("Failed to update customer details.");
        }
    }

    private void handleTransactionSummary() {
        List<Rental> rentals = rentalDao.getAllRentals();
        int totalCount = rentals.size();
        int activeCount = 0;
        int completedCount = 0;
        int cancelledCount = 0;
        double grossRevenue = 0.0;

        for (Rental r : rentals) {
            String st = r.status() == null ? "" : r.status().toUpperCase();
            if ("CANCELLED".equals(st) || "VOIDED".equals(st)) {
                cancelledCount++;
            } else {
                grossRevenue += r.totalAmount();
                if ("COMPLETED".equals(st)) {
                    completedCount++;
                } else {
                    activeCount++;
                }
            }
        }

        System.out.println("\n=======================================================");
        System.out.println("============= TRANSACTION SUMMARY REPORT ==============");
        System.out.println("=======================================================");
        System.out.printf("Total Transactions Recorded   : %d%n", totalCount);
        System.out.printf("Active / Ongoing Rentals      : %d%n", activeCount);
        System.out.printf("Completed Rentals             : %d%n", completedCount);
        System.out.printf("Cancelled / Voided Rentals    : %d%n", cancelledCount);
        System.out.printf("Total Active/Completed Revenue: P%.2f%n", grossRevenue);
        System.out.println("=======================================================");

        activityLogDao.logAction(currentAgent.userId(), "REPORT_SUMMARY",
                "Generated transaction summary report (" + totalCount + " transactions, Total: P" +
                        String.format("%.2f", grossRevenue) + ").");
    }

    private int extractVehicleId(String vehicleDetails) {
        if (vehicleDetails == null || !vehicleDetails.startsWith("[")) return -1;
        int endIdx = vehicleDetails.indexOf(']');
        if (endIdx <= 1) return -1;
        try {
            return Integer.parseInt(vehicleDetails.substring(1, endIdx).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void displayVehicleTable(List<Vehicle> vehicles) {
        System.out.println("\n==================================================================================");
        System.out.println("================================ VEHICLE INVENTORY ===============================");
        System.out.println("==================================================================================");
        System.out.printf("%-5s | %-15s | %-20s | %-15s | %-10s | %-12s%n",
                "ID", "Plate No.", "Model", "Category", "Rate", "Status");
        System.out.println("----------------------------------------------------------------------------------");
        if (vehicles.isEmpty()) {
            System.out.println("No matching vehicles found.");
        } else {
            for (Vehicle v : vehicles) {
                System.out.printf("%-5d | %-15s | %-20s | %-15s | P%-9.2f | %-12s%n",
                        v.vehicleId(), v.plateNumber(), v.model(), v.category(), v.dailyRate(), v.status());
            }
        }
        System.out.println("----------------------------------------------------------------------------------");
    }

    private void displayRentalTable(List<Rental> list) {
        System.out.println("\n=================================================================================================================");
        System.out.println("========================================== RENTAL RECORDS =======================================================");
        System.out.println("=================================================================================================================");
        System.out.printf("%-5s | %-8s | %-20s | %-25s | %-22s | %-10s | %-10s | %-20s%n",
                "ID", "User ID", "Customer", "Vehicle", "Type / Duration", "Amount", "Status", "Timestamp");
        System.out.println("-----------------------------------------------------------------------------------------------------------------");
        if (list.isEmpty()) {
            System.out.println("No rental records found.");
        } else {
            for (Rental t : list) {
                System.out.printf("%-5d | %-8d | %-20s | %-25s | %-22s | P%-9.2f | %-10s | %-20s%n",
                        t.rentalId(), t.userId(), t.customerName(), t.vehicleDetails(),
                        t.rentalType(), t.totalAmount(), t.status(), t.timestamp());
            }
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------");
    }
}