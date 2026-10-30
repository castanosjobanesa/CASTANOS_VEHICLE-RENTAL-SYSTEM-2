package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final UserDao userDao;
    private final Scanner scanner;
    private final User currentAdmin;

    public AdminMenu(UserDao userDao, Scanner scanner, User currentAdmin) {
        this.userDao = userDao;
        this.scanner = scanner;
        this.currentAdmin = currentAdmin;
    }

    public void start() {
        while (true) {
            System.out.println("\n===================================");
            System.out.println("=== SYSTEM ADMINISTRATOR MENU ===");
            System.out.println("=====================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending Users");
            System.out.println("3. Approve User");
            System.out.println("4. Update User Role & Status");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            int choice = readIntInput("");

            switch (choice) {
                case 1 -> displayAllUsers();
                case 2 -> displayPendingUsers();
                case 3 -> handleApproveUser();
                case 4 -> handleUpdateUser();
                case 5 -> {
                    System.out.println("Logging out from Admin session.");
                    return;
                }
                default -> System.out.println("Invalid choice. Please choose between 1 and 5.");
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

    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role (Rental Desk Agent / Fleet Supervisor / System Administrator) [0 to cancel]: ");
            String role = scanner.nextLine().trim();
            if (role.equals("0")) return null;

            if (role.equalsIgnoreCase("Rental Desk Agent") ||
                    role.equalsIgnoreCase("Fleet Supervisor") ||
                    role.equalsIgnoreCase("System Administrator") ||
                    role.equalsIgnoreCase("STUDENT") ||
                    role.equalsIgnoreCase("TEACHER") ||
                    role.equalsIgnoreCase("ADMIN")) {
                return role.toUpperCase();
            }
            System.out.println("Invalid role. Please enter a recognized role option.");
        }
    }

    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (APPROVED / PENDING) [0 to cancel]: ");
            String status = scanner.nextLine().trim();
            if (status.equals("0")) return null;

            if (status.equalsIgnoreCase("APPROVED") || status.equalsIgnoreCase("PENDING")) {
                return status.toUpperCase();
            }
            System.out.println("Invalid status. Please enter APPROVED or PENDING.");
        }
    }

    private void displayAllUsers() {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n==============================");
        System.out.println("==== ALL SYSTEM USERS ====");
        System.out.println("==============================");
        System.out.printf("%-5s | %-20s | %-25s | %-10s%n", "ID", "Username", "Role", "Status");
        System.out.println("------------------------------------------------------------------");
        for (User u : users) {
            System.out.printf("%-5d | %-20s | %-25s | %-10s%n",
                    u.userId(), u.username(), u.role(), u.status());
        }
    }

    private void displayPendingUsers() {
        List<User> users = userDao.getPendingUsers();
        System.out.println("\n--- PENDING USERS ---");
        if (users.isEmpty()) {
            System.out.println("No pending users found.");
            return;
        }
        System.out.printf("%-5s | %-20s | %-25s | %-10s%n", "No.", "Username", "Role", "Status");
        System.out.println("------------------------------------------------------------------");
        int displayNo = 1;
        for (User u : users) {
            System.out.printf("%-5d | %-20s | %-25s | %-10s%n",
                    displayNo++, u.username(), u.role(), u.status());
        }
    }

    private void handleApproveUser() {
        List<User> pending = userDao.getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("\n--- PENDING USERS ---");
            System.out.println("No pending users found.");
            return;
        }

        System.out.println("\n--- PENDING USERS ---");
        System.out.printf("%-5s | %-20s | %-25s | %-10s%n", "No.", "Username", "Role", "Status");
        System.out.println("------------------------------------------------------------------");
        int displayNo = 1;
        for (User u : pending) {
            System.out.printf("%-5d | %-20s | %-25s | %-10s%n",
                    displayNo++, u.username(), u.role(), u.status());
        }

        int selection = readIntInput("Enter number to approve (or 0 to cancel): ");
        if (selection == 0) return;

        if (selection < 1 || selection > pending.size()) {
            System.out.println("Invalid selection number.");
            return;
        }

        // Map the display number back to the actual User object's database ID
        User targetUser = pending.get(selection - 1);
        boolean success = userDao.approveUser(targetUser.userId());

        if (success) {
            System.out.println("User '" + targetUser.username() + "' successfully approved.");
        } else {
            System.out.println("Approval failed.");
        }
    }

    private void handleUpdateUser() {
        displayAllUsers();

        int targetUserId = readIntInput("\nEnter User ID to update (or 0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        if (targetUserId == currentAdmin.userId()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        boolean success = userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus);
        if (success) {
            System.out.println("User ID " + targetUserId + " updated successfully to Role: " + newRole + ", Status: " + newStatus);
        } else {
            System.out.println("Failed to update user. Please check if the User ID exists.");
        }
    }
}