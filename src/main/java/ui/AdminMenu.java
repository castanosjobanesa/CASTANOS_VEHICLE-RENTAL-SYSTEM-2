package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final UserDao userDao;
    private final Scanner scanner;
    private final User adminUser;

    public AdminMenu(UserDao userDao, Scanner scanner, User adminUser) {
        this.userDao = userDao;
        this.scanner = scanner;
        this.adminUser = adminUser;
    }

    public void start() {
        while (true) {
            System.out.println("\n=== SYSTEM ADMINISTRATOR MENU ===");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");

            String input = scanner.nextLine();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1 -> viewAllUsers();
                case 2 -> viewPendingUsers();
                case 3 -> approveUserAccount();
                case 4 -> {
                    System.out.println("Logging out from Admin menu.");
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();
        printUserTable(users, "ALL SYSTEM USERS");
    }

    private void viewPendingUsers() {
        List<User> users = userDao.getPendingUsers();
        printUserTable(users, "PENDING REGISTRATIONS");
    }

    private void printUserTable(List<User> users, String title) {
        System.out.println("\n--- " + title + " ---");
        if (users.isEmpty()) {
            System.out.println("No records found.");
            return;
        }

        // Table formatting using string formatters
        String format = "%-5s | %-15s | %-25s | %-10s%n";
        System.out.printf(format, "ID", "Username", "Role", "Status");
        System.out.println("-".repeat(65));

        for (User u : users) {
            System.out.printf(format, u.userId(), u.username(), u.role(), u.status());
        }
    }

    private void approveUserAccount() {
        List<User> pendingUsers = userDao.getPendingUsers();
        printUserTable(pendingUsers, "PENDING REGISTRATIONS AWAITING APPROVAL");

        if (pendingUsers.isEmpty()) {
            return;
        }

        System.out.print("\nEnter User ID to approve (or enter 0 to cancel): ");
        String input = scanner.nextLine();
        int targetId;
        try {
            targetId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
            return;
        }

        if (targetId == 0) {
            System.out.println("Approval action cancelled.");
            return;
        }

        // Verify if targetId exists in pending list
        boolean exists = pendingUsers.stream().anyMatch(u -> u.userId() == targetId);
        if (!exists) {
            System.out.println("Error: User ID " + targetId + " not found in pending registrations.");
            return;
        }

        boolean success = userDao.approveUser(targetId);
        if (success) {
            System.out.println("Success! User account (ID: " + targetId + ") status changed to APPROVED.");
        } else {
            System.out.println("Failed to approve user account.");
        }
    }
}