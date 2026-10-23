package com.scc.dao;

import com.scc.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    public boolean registerUser(String username, String passwordHash, String role) {
        // Check if there are ANY existing System Administrators in the database
        boolean isFirstAdmin = false;
        if ("System Administrator".equalsIgnoreCase(role)) {
            String countSql = "SELECT COUNT(*) FROM Users WHERE role = 'System Administrator'";
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    isFirstAdmin = true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // If it's the first admin, approve immediately. Otherwise, set to PENDING.
        String status = isFirstAdmin ? "APPROVED" : "PENDING";
        String sql = "INSERT INTO Users (username, password_hash, role, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, role);
            pstmt.setString(4, status);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public User login(String username, String passwordHash) {
        String sql = "SELECT * FROM Users WHERE username = ? AND password_hash = ? AND status = 'APPROVED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("status")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public List<User> getPendingUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public boolean approveUser(int userId) {
        // Parameterized PreparedStatement matching teacher's specification[cite: 4]
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}