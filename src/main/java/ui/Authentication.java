package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {
    private final UserDao userDao;
    private final Scanner scanner;
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        while (true) {
            System.out.println("\n=============================");
            System.out.println("=== VEHICLE RENTAL SYSTEM ===");
            System.out.println("=============================");
            System.out.println("1. Register User");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String input = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1 -> handleRegister();
                case 2 -> handleLogin();
                case 3 -> {
                    System.out.println("Exiting application.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public void handleRegister() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        String role = "";
        while (true) {
            System.out.println("\nSelect Role:");
            System.out.println("1. Rental Desk Agent");
            System.out.println("2. Fleet Supervisor");
            System.out.println("3. System Administrator");
            System.out.print("Enter choice (1-3): ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                role = "Rental Desk Agent";
                break;
            } else if (choice.equals("2")) {
                role = "Fleet Supervisor";
                break;
            } else if (choice.equals("3")) {
                role = "System Administrator";
                break;
            } else {
                System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }

        boolean success = userDao.registerUser(username, password, role);
        if (success) {
            if ("System Administrator".equalsIgnoreCase(role)) {
                System.out.println("User registered successfully! System Administrator is automatically approved.");
            } else {
                System.out.println("User registered successfully! Awaiting administrator approval.");
            }
        } else {
            System.out.println("Registration failed (Username may already exist).");
        }
    }

    public void handleLogin() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        User loggedUser = userDao.login(username, password);
        if (loggedUser != null) {
            activityLogDao.logAction(
                    loggedUser.userId(),
                    "AUTHENTICATION",
                    "User " + loggedUser.username() + " (ID: " + loggedUser.userId() + ") logged in."
            );
            System.out.println("Login successful! Welcome " + loggedUser.username() + " (" + loggedUser.role() + ")");

            if ("System Administrator".equalsIgnoreCase(loggedUser.role())) {
                AdminMenu adminMenu = new AdminMenu(userDao, scanner, loggedUser);
                adminMenu.start();
            } else if ("Rental Desk Agent".equalsIgnoreCase(loggedUser.role())) {
                RentalDeskMenu rentalMenu = new RentalDeskMenu(scanner, loggedUser);
                rentalMenu.start();
            } else if ("Fleet Supervisor".equalsIgnoreCase(loggedUser.role())) {
                FleetSupervisorMenu supervisorMenu = new FleetSupervisorMenu(scanner, loggedUser);
                supervisorMenu.start();
            } else {
                System.out.println("Dashboard for role '" + loggedUser.role() + "' is not implemented yet.");
            }

            activityLogDao.logAction(
                    loggedUser.userId(),
                    "AUTHENTICATION",
                    "User " + loggedUser.username() + " (ID: " + loggedUser.userId() + ") logged out."
            );
        } else {
            System.out.println("Invalid credentials or account is still pending approval.");
        }
    }
}