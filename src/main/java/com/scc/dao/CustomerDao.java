package com.scc.dao;

import java.sql.*;

public class CustomerDao {
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public boolean registerCustomer(int userId, String fullName, String licenseNumber, String contactNumber, String address) {
        String sql = "INSERT INTO Customers (full_name, license_number, contact_number, address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, licenseNumber);
            pstmt.setString(3, contactNumber);
            pstmt.setString(4, address);
            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int customerId = rs.getInt(1);
                        activityLogDao.logAction(userId, "CUSTOMER_REGISTRATION", "Registered customer profile for " + fullName + " (ID: " + customerId + ")");
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateCustomerContact(int userId, int customerId, String newContact, String newAddress) {
        String sql = "UPDATE Customers SET contact_number = ?, address = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newContact);
            pstmt.setString(2, newAddress);
            pstmt.setInt(3, customerId);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                activityLogDao.logAction(userId, "CUSTOMER_UPDATED", "Updated contact details for customer ID " + customerId);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}