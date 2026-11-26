package com.scc.dao;

import com.scc.model.Rental;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RentalDao {
    private static final String DB_URL = "jdbc:sqlite:vehicle_rental.db";
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public RentalDao() {
        initializeRentalsTable();
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private void initializeRentalsTable() {
        String createSql = """
                CREATE TABLE IF NOT EXISTS rentals (
                    rental_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    vehicle_id INTEGER DEFAULT 0,
                    customer_name TEXT NOT NULL,
                    vehicle_details TEXT NOT NULL,
                    rental_type TEXT NOT NULL,
                    rental_days INTEGER DEFAULT 1,
                    daily_rate REAL DEFAULT 0.0,
                    total_amount REAL NOT NULL,
                    return_date TEXT,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    timestamp TEXT NOT NULL
                )
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);

            addColumnIfMissing(stmt, "ALTER TABLE rentals ADD COLUMN vehicle_id INTEGER DEFAULT 0");
            addColumnIfMissing(stmt, "ALTER TABLE rentals ADD COLUMN rental_days INTEGER DEFAULT 1");
            addColumnIfMissing(stmt, "ALTER TABLE rentals ADD COLUMN daily_rate REAL DEFAULT 0.0");
            addColumnIfMissing(stmt, "ALTER TABLE rentals ADD COLUMN return_date TEXT");
        } catch (SQLException e) {
            System.out.println("Error initializing rentals table: " + e.getMessage());
        }
    }

    private void addColumnIfMissing(Statement stmt, String alterSql) {
        try {
            stmt.execute(alterSql);
        } catch (SQLException ignored) {
        }
    }

    private String resolveIdColumn(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(rentals)")) {
            while (rs.next()) {
                if ("rental_id".equalsIgnoreCase(rs.getString("name"))) {
                    return "rental_id";
                }
            }
        } catch (SQLException ignored) {
        }
        return "id";
    }

    public boolean createRentalTransaction(int userId, String customerName, String vehicleDetails, String rentalType, double totalAmount) {
        int vehicleId = parseVehicleId(vehicleDetails);
        int rentalDays = parseDays(rentalType);
        double dailyRate = rentalDays > 0 ? totalAmount / rentalDays : totalAmount;
        String returnDate = parseReturnDate(rentalType, rentalDays);
        String now = LocalDateTime.now().format(TS_FORMAT);

        String sql = """
                INSERT INTO rentals (
                    user_id, vehicle_id, customer_name, vehicle_details,
                    rental_type, rental_days, daily_rate, total_amount,
                    return_date, status, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, vehicleId);
            pstmt.setString(3, customerName);
            pstmt.setString(4, vehicleDetails);
            pstmt.setString(5, rentalType);
            pstmt.setInt(6, rentalDays);
            pstmt.setDouble(7, dailyRate);
            pstmt.setDouble(8, totalAmount);
            pstmt.setString(9, returnDate);
            pstmt.setString(10, now);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error creating rental transaction: " + e.getMessage());
            return false;
        }
    }

    public List<Rental> getAllRentals() {
        List<Rental> rentals = new ArrayList<>();

        try (Connection conn = getConnection()) {
            String idCol = resolveIdColumn(conn);
            String sql = "SELECT * FROM rentals ORDER BY " + idCol + " DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rentals.add(mapRowToRental(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving rentals: " + e.getMessage());
        }
        return rentals;
    }

    public List<Rental> searchRentals(String keyword) {
        List<Rental> rentals = new ArrayList<>();

        try (Connection conn = getConnection()) {
            String idCol = resolveIdColumn(conn);
            String sql = "SELECT * FROM rentals WHERE customer_name LIKE ? OR CAST(" + idCol + " AS TEXT) = ? ORDER BY " + idCol + " DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + keyword + "%");
                pstmt.setString(2, keyword);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        rentals.add(mapRowToRental(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error searching rentals: " + e.getMessage());
        }
        return rentals;
    }

    public boolean updateRentalStatus(int userId, int rentalId, String status) {
        try (Connection conn = getConnection()) {
            String idCol = resolveIdColumn(conn);
            String sql = "UPDATE rentals SET status = ? WHERE " + idCol + " = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status.toUpperCase());
                pstmt.setInt(2, rentalId);
                boolean updated = pstmt.executeUpdate() > 0;
                if (updated) {
                    activityLogDao.logAction(userId, "RENTAL_STATUS",
                            "Updated rental #" + rentalId + " status to " + status.toUpperCase() + ".");
                }
                return updated;
            }
        } catch (SQLException e) {
            System.out.println("Error updating rental status: " + e.getMessage());
            return false;
        }
    }

    public boolean extendRentalDuration(int userId, int rentalId, int extraDays, String newReturnDate, double updatedTotal) {
        try (Connection conn = getConnection()) {
            String idCol = resolveIdColumn(conn);
            String sql = "UPDATE rentals SET rental_days = COALESCE(rental_days, 1) + ?, return_date = ?, total_amount = ?, rental_type = ? WHERE " + idCol + " = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, extraDays);
                pstmt.setString(2, newReturnDate);
                pstmt.setDouble(3, updatedTotal);
                pstmt.setString(4, "EXTENDED (+" + extraDays + "d, Return: " + newReturnDate + ")");
                pstmt.setInt(5, rentalId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error extending rental duration: " + e.getMessage());
            return false;
        }
    }

    public boolean cancelOrVoidRental(int userId, int rentalId) {
        return updateRentalStatus(userId, rentalId, "CANCELLED");
    }

    private Rental mapRowToRental(ResultSet rs) throws SQLException {
        int idVal = hasColumn(rs, "rental_id") ? rs.getInt("rental_id") : rs.getInt("id");
        String returnDate = hasColumn(rs, "return_date") && rs.getString("return_date") != null
                ? rs.getString("return_date")
                : "";
        String rawStatus = rs.getString("status");
        String effectiveStatus = resolveOverdueStatus(rawStatus, returnDate);

        return new Rental(
                idVal,
                rs.getInt("user_id"),
                hasColumn(rs, "vehicle_id") ? rs.getInt("vehicle_id") : 0,
                rs.getString("customer_name"),
                rs.getString("vehicle_details"),
                rs.getString("rental_type"),
                hasColumn(rs, "rental_days") ? rs.getInt("rental_days") : 1,
                hasColumn(rs, "daily_rate") ? rs.getDouble("daily_rate") : 0.0,
                rs.getDouble("total_amount"),
                returnDate,
                effectiveStatus,
                rs.getString("timestamp")
        );
    }

    private String resolveOverdueStatus(String rawStatus, String returnDate) {
        if (rawStatus == null) return "ACTIVE";
        String upper = rawStatus.toUpperCase();
        if ("ACTIVE".equals(upper) && returnDate != null && !returnDate.isBlank()) {
            try {
                LocalDate due = LocalDate.parse(returnDate.trim());
                if (LocalDate.now().isAfter(due)) {
                    return "OVERDUE";
                }
            } catch (Exception ignored) {
            }
        }
        return upper;
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private int parseVehicleId(String vehicleDetails) {
        if (vehicleDetails == null || !vehicleDetails.startsWith("[")) return 0;
        int endIdx = vehicleDetails.indexOf(']');
        if (endIdx <= 1) return 0;
        try {
            return Integer.parseInt(vehicleDetails.substring(1, endIdx).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseDays(String rentalType) {
        if (rentalType == null) return 1;
        int openParen = rentalType.indexOf('(');
        int dIdx = rentalType.indexOf("d,", openParen);
        if (openParen != -1 && dIdx > openParen) {
            try {
                return Integer.parseInt(rentalType.substring(openParen + 1, dIdx).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    private String parseReturnDate(String rentalType, int rentalDays) {
        if (rentalType != null && rentalType.contains("Return: ")) {
            int start = rentalType.indexOf("Return: ") + 8;
            int end = rentalType.indexOf(')', start);
            if (end > start) {
                return rentalType.substring(start, end).trim();
            }
        }
        return LocalDate.now().plusDays(Math.max(1, rentalDays)).toString();
    }
}