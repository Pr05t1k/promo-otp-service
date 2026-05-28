package com.promo.otp.dao;

import com.promo.otp.config.DatabaseConfig;
import com.promo.otp.model.Role;
import com.promo.otp.model.User;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    public boolean createUser(User user, String plainPassword) {
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        String sql = "INSERT INTO users (login, password_hash, role, email, phone, telegram_chat_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getLogin());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.getRole().name());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getPhone());
            stmt.setString(6, user.getTelegramChatId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                log.info("User created: {}", user.getLogin());
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error creating user: {}", e.getMessage());
            return false;
        }
    }

    public User findByLogin(String login) {
        String sql = "SELECT * FROM users WHERE login = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            log.error("Error finding user by login: {}", e.getMessage());
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            log.error("Error finding user by id: {}", e.getMessage());
        }
        return null;
    }

    public boolean checkPassword(User user, String plainPassword) {
        return BCrypt.checkpw(plainPassword, user.getPasswordHash());
    }

    public boolean adminExists() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("Error checking admin existence: {}", e.getMessage());
        }
        return false;
    }

    public List<User> getAllNonAdminUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'USER' ORDER BY id";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            log.error("Error getting all users: {}", e.getMessage());
        }
        return users;
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ? AND role = 'USER'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                log.info("User deleted with id: {}", userId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error deleting user: {}", e.getMessage());
            return false;
        }
    }

    public boolean updateTelegramChatId(int userId, String chatId) {
        String sql = "UPDATE users SET telegram_chat_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, chatId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating telegram chat id: {}", e.getMessage());
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setLogin(rs.getString("login"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setTelegramChatId(rs.getString("telegram_chat_id"));
        
        if (rs.getTimestamp("created_at") != null) {
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return user;
    }
}