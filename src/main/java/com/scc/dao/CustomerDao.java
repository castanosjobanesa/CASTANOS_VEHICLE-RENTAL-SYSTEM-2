package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class CustomerDao {
    private static final String DB_URL = "jdbc:sqlite:vehicle_rental.db";

    public CustomerDao() {
        initializeCustomersTable();
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private void initializeCustomersTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    license_number TEXT UNIQUE NOT NULL,
                    contact_number TEXT NOT NULL,
                    address TEXT NOT NULL
                )
                """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Error initializing customers table: " + e.getMessage());
        }
    }

    public boolean registerCustomer(int userId, String fullName, String licenseNumber, String contactNumber, String address) {
        String sql = """
                INSERT OR IGNORE INTO customers (full_name, license_number, contact_number, address)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, licenseNumber);
            pstmt.setString(3, contactNumber);
            pstmt.setString(4, address);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error registering customer: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCustomerContact(int userId, int customerId, String newContact, String newAddress) {
        String sql = "UPDATE customers SET contact_number = ?, address = ? WHERE customer_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newContact);
            pstmt.setString(2, newAddress);
            pstmt.setInt(3, customerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating customer contact: " + e.getMessage());
            return false;
        }
    }
}