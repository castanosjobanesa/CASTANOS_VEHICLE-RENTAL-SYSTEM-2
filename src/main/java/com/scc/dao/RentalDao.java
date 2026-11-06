package com.scc.dao;

import com.scc.model.Rental;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RentalDao {
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public boolean createRentalTransaction(int userId, String customerName, String vehicleDetails, String rentalType, double totalAmount) {
        String sql = "INSERT INTO rentals (user_id, customer_name, vehicle_details, rental_type, total_amount, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, customerName);
            pstmt.setString(3, vehicleDetails);
            pstmt.setString(4, rentalType);
            pstmt.setDouble(5, totalAmount);
            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int rentalId = rs.getInt(1);
                        activityLogDao.logAction(userId, "VEHICLE_RENTAL", "Processed rental for Customer " + customerName + " - Total: P" + totalAmount);
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateRentalStatus(int userId, int rentalId, String newStatus) {
        String oldStatus = "ACTIVE";
        String selectSql = "SELECT status FROM rentals WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmtSelect = conn.prepareStatement(selectSql)) {
            pstmtSelect.setInt(1, rentalId);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                if (rs.next()) {
                    oldStatus = rs.getString("status");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sql = "UPDATE rentals SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.toUpperCase());
            pstmt.setInt(2, rentalId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                String actionType = "COMPLETED".equalsIgnoreCase(newStatus) ? "VEHICLE_RETURN" : "RENTAL_CANCELLATION";
                activityLogDao.logAction(userId, actionType, "Updated rental #" + rentalId + " status from " + oldStatus + " to " + newStatus.toUpperCase());
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Rental> searchRentals(String keyword) {
        List<Rental> rentals = new ArrayList<>();
        String sql = "SELECT * FROM rentals WHERE customer_name LIKE ? OR CAST(id AS TEXT) LIKE ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rentals.add(new Rental(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("customer_name"),
                            rs.getString("vehicle_details"),
                            rs.getString("rental_type"),
                            rs.getDouble("total_amount"),
                            rs.getString("status"),
                            rs.getString("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rentals;
    }

    public List<Rental> getAllRentals() {
        List<Rental> rentals = new ArrayList<>();
        String sql = "SELECT * FROM rentals ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rentals.add(new Rental(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("customer_name"),
                        rs.getString("vehicle_details"),
                        rs.getString("rental_type"),
                        rs.getDouble("total_amount"),
                        rs.getString("status"),
                        rs.getString("timestamp")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rentals;
    }
}