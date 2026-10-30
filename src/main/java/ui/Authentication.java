package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {
    private final UserDao userDao;
    private final Scanner scanner;

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

            String input = scanner.nextLine();
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
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String role = "";
        while (true) {
            System.out.println("\nSelect Role:");
            System.out.println("1. Rental Desk Agent");
            System.out.println("2. Fleet Supervisor");
            System.out.println("3. System Administrator");
            System.out.print("Enter choice (1-3): ");

            String choice = scanner.nextLine();

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
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        User loggedUser = userDao.login(username, password);
        if (loggedUser != null) {
            System.out.println("Login successful! Welcome " + loggedUser.username() + " (" + loggedUser.role() + ")");

            // Route to appropriate role-specific menu
            if ("System Administrator".equalsIgnoreCase(loggedUser.role())) {
                AdminMenu adminMenu = new AdminMenu(userDao, scanner, loggedUser);
                adminMenu.start();
            } else {
                System.out.println("Dashboard for role '" + loggedUser.role() + "' is not implemented yet.");
            }
        } else {
            System.out.println("Invalid credentials or account is still pending approval.");
        }
    }
}