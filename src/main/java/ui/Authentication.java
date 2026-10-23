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
            System.out.println("\n=== VEHICLE RENTAL SYSTEM ===");
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
        System.out.print("Enter role (Rental Desk Agent / Fleet Supervisor / System Administrator): ");
        String role = scanner.nextLine();

        boolean success = userDao.registerUser(username, password, role);
        if (success) {
            System.out.println("User registered successfully! Awaiting administrator approval.");
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