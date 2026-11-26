package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.VehicleDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;
import com.scc.model.Vehicle;

import java.util.List;
import java.util.Scanner;

public class FleetSupervisorMenu {
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final Scanner scanner;
    private final User currentSupervisor;

    public FleetSupervisorMenu(Scanner scanner, User currentSupervisor) {
        this.scanner = scanner;
        this.currentSupervisor = currentSupervisor;
    }

    public void start() {
        while (true) {
            System.out.println("\n=======================================================");
            System.out.println("=============== FLEET SUPERVISOR MENU =================");
            System.out.println("=======================================================");
            System.out.println("1. Add New Vehicle to Fleet ");
            System.out.println("2. View / Filter Fleet Inventory ");
            System.out.println("3. Update Vehicle Status [AVAILABLE / RENTED / UNDER SERVICE] ");
            System.out.println("4. Deactivate / Soft-Delete Retired or Damaged Vehicle ");
            System.out.println("5. View Staff Action / Audit Logs");
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");

            int choice = readIntInput("");
            switch (choice) {
                case 1 -> handleAddVehicle();
                case 2 -> handleViewVehicles();
                case 3 -> handleUpdateVehicleStatus();
                case 4 -> handleDeactivateVehicle();
                case 5 -> handleViewLogs();
                case 6 -> {
                    System.out.println("Logging out from Fleet Supervisor session.");
                    return;
                }
                default -> System.out.println("Invalid choice. Please choose between 1 and 6.");
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

    private void handleAddVehicle() {
        System.out.println("\n--- 1. ADD NEW VEHICLE ---");
        System.out.print("Enter Plate Number (e.g., ABC-1234): ");
        String plate = scanner.nextLine().trim();
        System.out.print("Enter Vehicle Model (e.g., Toyota Vios): ");
        String model = scanner.nextLine().trim();
        System.out.print("Enter Category (e.g., Sedan / SUV / Van): ");
        String category = scanner.nextLine().trim();

        double rate = 0;
        while (true) {
            System.out.print("Enter Daily Rate (P): ");
            try {
                rate = Double.parseDouble(scanner.nextLine().trim());
                if (rate > 0) break;
                System.out.println("Daily rate must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid rate. Please enter a valid number.");
            }
        }

        boolean added = vehicleDao.addVehicle(currentSupervisor.userId(), plate, model, category, rate, "AVAILABLE");
        if (added) {
            System.out.println("Vehicle added with status AVAILABLE!");
        } else {
            System.out.println("Failed to add vehicle (Plate number may already exist).");
        }
    }

    private void handleViewVehicles() {
        System.out.println("\n--- 2. FLEET INVENTORY ---");
        System.out.print("Enter Category filter (blank for all): ");
        String categoryFilter = scanner.nextLine().trim();
        System.out.print("Enter Status filter [AVAILABLE / RENTED / UNDER SERVICE / DEACTIVATED] (blank for active fleet): ");
        String statusFilter = scanner.nextLine().trim();

        List<Vehicle> vehicles = vehicleDao.getFilteredVehicles(categoryFilter, statusFilter);
        System.out.println("\n==================================================================================");
        System.out.printf("%-5s | %-15s | %-20s | %-15s | %-10s | %-12s%n",
                "ID", "Plate No.", "Model", "Category", "Rate", "Status");
        System.out.println("----------------------------------------------------------------------------------");
        if (vehicles.isEmpty()) {
            System.out.println("No vehicles found.");
        } else {
            for (Vehicle v : vehicles) {
                System.out.printf("%-5d | %-15s | %-20s | %-15s | P%-9.2f | %-12s%n",
                        v.vehicleId(), v.plateNumber(), v.model(), v.category(), v.dailyRate(), v.status());
            }
        }
        System.out.println("----------------------------------------------------------------------------------");
    }

    private void handleUpdateVehicleStatus() {
        System.out.println("\n--- 3. UPDATE VEHICLE STATUS ---");
        List<Vehicle> vehicles = vehicleDao.getFilteredVehicles("", "");
        if (vehicles.isEmpty()) {
            System.out.println("No vehicles in inventory.");
            return;
        }

        System.out.printf("%-5s | %-15s | %-20s | %-12s%n", "ID", "Plate No.", "Model", "Status");
        System.out.println("------------------------------------------------------------");
        for (Vehicle v : vehicles) {
            System.out.printf("%-5d | %-15s | %-20s | %-12s%n",
                    v.vehicleId(), v.plateNumber(), v.model(), v.status());
        }

        int vehicleId = readIntInput("\nEnter Vehicle ID to update (or 0 to cancel): ");
        if (vehicleId == 0) return;

        System.out.println("Select new status:");
        System.out.println("1. AVAILABLE");
        System.out.println("2. RENTED");
        System.out.println("3. UNDER SERVICE");
        int statusChoice = readIntInput("Choose (1-3): ");

        String newStatus = switch (statusChoice) {
            case 1 -> "AVAILABLE";
            case 2 -> "RENTED";
            case 3 -> "UNDER SERVICE";
            default -> null;
        };

        if (newStatus == null) {
            System.out.println("Invalid status selection.");
            return;
        }

        boolean updated = vehicleDao.updateVehicleStatus(currentSupervisor.userId(), vehicleId, newStatus);
        if (updated) {
            System.out.println("Vehicle #" + vehicleId + " updated to " + newStatus + ".");
        } else {
            System.out.println("Failed to update vehicle status.");
        }
    }

    private void handleDeactivateVehicle() {
        System.out.println("\n--- 4. DEACTIVATE / SOFT-DELETE VEHICLE ---");
        List<Vehicle> vehicles = vehicleDao.getFilteredVehicles("", "");
        if (vehicles.isEmpty()) {
            System.out.println("No active vehicles available to deactivate.");
            return;
        }

        System.out.printf("%-5s | %-15s | %-20s | %-12s%n", "ID", "Plate No.", "Model", "Status");
        System.out.println("------------------------------------------------------------");
        for (Vehicle v : vehicles) {
            System.out.printf("%-5d | %-15s | %-20s | %-12s%n",
                    v.vehicleId(), v.plateNumber(), v.model(), v.status());
        }

        int vehicleId = readIntInput("\nEnter Vehicle ID to deactivate (or 0 to cancel): ");
        if (vehicleId == 0) return;

        System.out.print("Enter reason (e.g., Retired / Major Damage): ");
        String reason = scanner.nextLine().trim();

        boolean deactivated = vehicleDao.deactivateVehicle(currentSupervisor.userId(), vehicleId, reason);
        if (deactivated) {
            System.out.println("Vehicle #" + vehicleId + " marked as DEACTIVATED (removed from rental availability).");
        } else {
            System.out.println("Could not deactivate vehicle (ensure it exists and is not currently RENTED).");
        }
    }

    private void handleViewLogs() {
        List<ActivityLog> logs = activityLogDao.getAllLogs();
        System.out.println("\n--- AUDIT LOGS ---");
        if (logs.isEmpty()) {
            System.out.println("No logs found.");
            return;
        }
        for (ActivityLog log : logs) {
            System.out.printf("[%s] User#%d | %s | %s%n",
                    log.timestamp(), log.userId(), log.actionType(), log.description());
        }
    }
}