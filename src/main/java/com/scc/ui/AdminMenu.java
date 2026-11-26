package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final UserDao userDao;
    private final Scanner scanner;
    private final User currentAdmin;
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

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
            System.out.println("5. View System Audit Logs");
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");

            int choice = readIntInput("");

            switch (choice) {
                case 1 -> displayAllUsers();
                case 2 -> displayPendingUsers();
                case 3 -> handleApproveUser();
                case 4 -> handleUpdateUser();
                case 5 -> handleViewAuditLogsMenu();
                case 6 -> {
                    System.out.println("Logging out from Admin session.");
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

        User targetUser = pending.get(selection - 1);
        boolean success = userDao.approveUser(currentAdmin.userId(), targetUser.userId());

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

        boolean success = userDao.updateUserRoleAndStatus(currentAdmin.userId(), targetUserId, newRole, newStatus);
        if (success) {
            System.out.println("User ID " + targetUserId + " updated successfully to Role: " + newRole + ", Status: " + newStatus);
        } else {
            System.out.println("Failed to update user. Please check if the User ID exists.");
        }
    }

    private void handleViewAuditLogsMenu() {
        while (true) {
            System.out.println("\n=================================");
            System.out.println("=== AUDIT LOGS MANAGEMENT ===");
            System.out.println("=================================");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Back to Admin Menu");
            System.out.print("Choose an option: ");

            int choice = readIntInput("");
            switch (choice) {
                case 1 -> displayAuditLogs(activityLogDao.getAllLogs());
                case 2 -> {
                    int filterUserId = readIntInput("Enter User ID to filter by: ");
                    displayAuditLogs(activityLogDao.getLogsByUserId(filterUserId));
                }
                case 3 -> {
                    return;
                }
                default -> System.out.println("Invalid choice. Please choose between 1 and 3.");
            }
        }
    }

    private void displayAuditLogs(List<ActivityLog> logs) {
        System.out.println("\n=========================================================================================================================================");
        System.out.println("========================================================= SYSTEM AUDIT LOGS =============================================================");
        System.out.println("=========================================================================================================================================");
        System.out.printf("%-5s | %-8s | %-22s | %-65s | %-20s%n", "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------");
        if (logs.isEmpty()) {
            System.out.println("No audit logs found.");
        } else {
            for (ActivityLog log : logs) {
                System.out.printf("%-5s | %-8s | %-22s | %-65s | %-20s%n",
                        log.id(), log.userId(), log.actionType(), log.description(), log.timestamp());
            }
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Total Audit Logs Recorded: " + logs.size());
        System.out.println("=========================================================================================================================================");
    }
}