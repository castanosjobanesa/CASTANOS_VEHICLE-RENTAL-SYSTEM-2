package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initializeDatabase() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS Users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING'
            );
            """;

        String createVehiclesTable = """
            CREATE TABLE IF NOT EXISTS Vehicles (
                vehicle_id INTEGER PRIMARY KEY AUTOINCREMENT,
                plate_number TEXT UNIQUE NOT NULL,
                model TEXT NOT NULL,
                category TEXT NOT NULL,
                daily_rate REAL NOT NULL,
                status TEXT DEFAULT 'AVAILABLE'
            );
            """;

        String createCustomersTable = """
            CREATE TABLE IF NOT EXISTS Customers (
                customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                license_number TEXT UNIQUE NOT NULL,
                contact_number TEXT NOT NULL,
                address TEXT NOT NULL
            );
            """;

        String createActivityLogsTable = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action_type TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
            );
            """;

        String createRentalsTable = """
            CREATE TABLE IF NOT EXISTS rentals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                customer_name TEXT NOT NULL,
                vehicle_details TEXT NOT NULL,
                rental_type TEXT NOT NULL,
                total_amount REAL NOT NULL,
                status TEXT DEFAULT 'ACTIVE',
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
            );
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createVehiclesTable);
            stmt.execute(createCustomersTable);
            stmt.execute(createActivityLogsTable);
            stmt.execute(createRentalsTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}