package com.promo.otp.dao;

import com.promo.otp.config.DatabaseConfig;
import com.promo.otp.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class OtpConfigDao {
    private static final Logger log = LoggerFactory.getLogger(OtpConfigDao.class);

    public OtpConfig getConfig() {
        String sql = "SELECT * FROM otp_config LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return mapConfig(rs);
            }
        } catch (SQLException e) {
            log.error("Error getting config: {}", e.getMessage());
        }
        
        // Return default config if not found
        OtpConfig defaultConfig = new OtpConfig();
        defaultConfig.setId(1);
        return defaultConfig;
    }

    public boolean updateConfig(int codeLength, int ttlSeconds) {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, codeLength);
            stmt.setInt(2, ttlSeconds);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                log.info("OTP config updated: codeLength={}, ttlSeconds={}", codeLength, ttlSeconds);
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error updating config: {}", e.getMessage());
            return false;
        }
    }

    private OtpConfig mapConfig(ResultSet rs) throws SQLException {
        OtpConfig config = new OtpConfig();
        config.setId(rs.getInt("id"));
        config.setCodeLength(rs.getInt("code_length"));
        config.setTtlSeconds(rs.getInt("ttl_seconds"));
        if (rs.getTimestamp("updated_at") != null) {
            config.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return config;
    }
}