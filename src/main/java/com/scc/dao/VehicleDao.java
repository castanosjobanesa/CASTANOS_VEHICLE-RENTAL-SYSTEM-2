package com.scc.dao;

import com.scc.model.Vehicle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {
    private static final String DB_URL = "jdbc:sqlite:vehicle_rental.db";
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public VehicleDao() {
        initializeVehiclesTable();
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private void initializeVehiclesTable() {
        String createSql = """
                CREATE TABLE IF NOT EXISTS vehicles (
                    vehicle_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plate_number TEXT UNIQUE NOT NULL,
                    model TEXT NOT NULL,
                    category TEXT NOT NULL,
                    daily_rate REAL NOT NULL,
                    status TEXT NOT NULL DEFAULT 'AVAILABLE'
                )
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
        } catch (SQLException e) {
            System.out.println("Error initializing vehicles table: " + e.getMessage());
        }
    }

    public boolean addVehicle(int userId, String plateNumber, String model, String category, double dailyRate, String status) {
        String sql = """
                INSERT OR IGNORE INTO vehicles (plate_number, model, category, daily_rate, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plateNumber.toUpperCase());
            pstmt.setString(2, model);
            pstmt.setString(3, category);
            pstmt.setDouble(4, dailyRate);
            pstmt.setString(5, status.toUpperCase());

            boolean inserted = pstmt.executeUpdate() > 0;
            if (inserted) {
                activityLogDao.logAction(userId, "VEHICLE_CREATE",
                        "Added vehicle " + model + " (" + plateNumber.toUpperCase() + ") - Status: " + status.toUpperCase());
            }
            return inserted;
        } catch (SQLException e) {
            System.out.println("Error adding vehicle: " + e.getMessage());
            return false;
        }
    }

    public List<Vehicle> getFilteredVehicles(String categoryFilter, String statusFilter) {
        List<Vehicle> vehicles = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM vehicles WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (categoryFilter != null && !categoryFilter.isBlank()) {
            sql.append(" AND LOWER(category) LIKE ?");
            params.add("%" + categoryFilter.trim().toLowerCase() + "%");
        }

        if (statusFilter != null && !statusFilter.isBlank()) {
            sql.append(" AND UPPER(status) = ?");
            params.add(statusFilter.trim().toUpperCase());
        } else {
            // Hide soft-deleted/deactivated vehicles unless explicitly filtered
            sql.append(" AND UPPER(status) <> 'DEACTIVATED'");
        }

        sql.append(" ORDER BY vehicle_id ASC");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(mapRowToVehicle(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error loading vehicles: " + e.getMessage());
        }
        return vehicles;
    }

    public boolean updateVehicleStatus(int userId, int vehicleId, String newStatus) {
        String idCol = resolveVehicleIdColumn();
        String sql = "UPDATE vehicles SET status = ? WHERE " + idCol + " = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.toUpperCase());
            pstmt.setInt(2, vehicleId);
            boolean updated = pstmt.executeUpdate() > 0;
            if (updated) {
                activityLogDao.logAction(userId, "VEHICLE_STATUS",
                        "Updated vehicle #" + vehicleId + " status to " + newStatus.toUpperCase() + ".");
            }
            return updated;
        } catch (SQLException e) {
            System.out.println("Error updating vehicle status: " + e.getMessage());
            return false;
        }
    }

    public boolean deactivateVehicle(int userId, int vehicleId, String reason) {
        String idCol = resolveVehicleIdColumn();
        String sql = "UPDATE vehicles SET status = 'DEACTIVATED' WHERE " + idCol + " = ? AND UPPER(status) <> 'RENTED'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vehicleId);
            boolean updated = pstmt.executeUpdate() > 0;
            if (updated) {
                String note = (reason == null || reason.isBlank()) ? "Retired/damaged" : reason.trim();
                activityLogDao.logAction(userId, "VEHICLE_DEACTIVATE",
                        "Deactivated vehicle #" + vehicleId + " (" + note + ").");
            }
            return updated;
        } catch (SQLException e) {
            System.out.println("Error deactivating vehicle: " + e.getMessage());
            return false;
        }
    }

    private String resolveVehicleIdColumn() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(vehicles)")) {
            while (rs.next()) {
                if ("vehicle_id".equalsIgnoreCase(rs.getString("name"))) {
                    return "vehicle_id";
                }
            }
        } catch (SQLException ignored) {
        }
        return "id";
    }

    private Vehicle mapRowToVehicle(ResultSet rs) throws SQLException {
        int idVal = hasColumn(rs, "vehicle_id") ? rs.getInt("vehicle_id") : rs.getInt("id");
        return new Vehicle(
                idVal,
                rs.getString("plate_number"),
                rs.getString("model"),
                rs.getString("category"),
                rs.getDouble("daily_rate"),
                rs.getString("status")
        );
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}