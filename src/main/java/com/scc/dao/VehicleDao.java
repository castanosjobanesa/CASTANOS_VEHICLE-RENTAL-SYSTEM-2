package com.scc.dao;

import com.scc.model.Vehicle;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public boolean addVehicle(int userId, String plateNumber, String model, String category, double dailyRate) {
        String sql = "INSERT INTO Vehicles (plate_number, model, category, daily_rate, status) VALUES (?, ?, ?, ?, 'AVAILABLE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plateNumber);
            pstmt.setString(2, model);
            pstmt.setString(3, category);
            pstmt.setDouble(4, dailyRate);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                activityLogDao.logAction(userId, "VEHICLE_ADDED", "Added vehicle model: " + model + " (" + plateNumber + ")");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateVehicleStatus(int userId, int vehicleId, String newStatus) {
        String sql = "UPDATE Vehicles SET status = ? WHERE vehicle_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.toUpperCase());
            pstmt.setInt(2, vehicleId);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                activityLogDao.logAction(userId, "VEHICLE_STATUS_UPDATE", "Updated vehicle ID " + vehicleId + " operational status to " + newStatus.toUpperCase());
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Vehicle> getFilteredVehicles(String typeFilter, String statusFilter) {
        List<Vehicle> vehicles = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM Vehicles WHERE 1=1");
        if (typeFilter != null && !typeFilter.isEmpty()) {
            sql.append(" AND category LIKE ?");
        }
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append(" AND status = ?");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (typeFilter != null && !typeFilter.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + typeFilter + "%");
            }
            if (statusFilter != null && !statusFilter.isEmpty()) {
                pstmt.setString(paramIndex++, statusFilter.toUpperCase());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(new Vehicle(
                            rs.getInt("vehicle_id"),
                            rs.getString("plate_number"),
                            rs.getString("model"),
                            rs.getString("category"),
                            rs.getDouble("daily_rate"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vehicles;
    }
}