package com.scc.dao;

import com.scc.model.User;
import java.sql.*;

public class UserDao {
    public boolean registerUser(String username, String passwordHash, String role) {
        // Updated to insert with 'PENDING' status as required
        String sql = "INSERT INTO Users (username, password_hash, role, status) VALUES (?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, role);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public User login(String username, String passwordHash) {
        // Note: Depending on your system flow, if pending users cannot login yet,
        // you can change 'Active' to 'PENDING' or keep it as needed.
        // Typically, login checks for 'Active' or allows 'PENDING' if auto-approved.
        String sql = "SELECT * FROM Users WHERE username = ? AND password_hash = ? AND status = 'Active'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
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
}