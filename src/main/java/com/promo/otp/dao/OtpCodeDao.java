package com.promo.otp.dao;

import com.promo.otp.config.DatabaseConfig;
import com.promo.otp.model.DeliveryMethod;
import com.promo.otp.model.OtpCode;
import com.promo.otp.model.OtpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OtpCodeDao {
    private static final Logger log = LoggerFactory.getLogger(OtpCodeDao.class);

    public boolean saveOtpCode(OtpCode otpCode) {
        String sql = "INSERT INTO otp_codes (operation_id, user_id, code, status, expires_at, delivery_method) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, otpCode.getOperationId());
            stmt.setInt(2, otpCode.getUserId());
            stmt.setString(3, otpCode.getCode());
            stmt.setString(4, otpCode.getStatus().name());
            stmt.setTimestamp(5, Timestamp.valueOf(otpCode.getExpiresAt()));
            stmt.setString(6, otpCode.getDeliveryMethod().name());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        otpCode.setId(generatedKeys.getInt(1));
                    }
                }
                log.info("OTP code saved for operation: {}", otpCode.getOperationId());
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error saving OTP code: {}", e.getMessage());
            return false;
        }
    }

    public OtpCode findByOperationIdAndUserId(String operationId, int userId) {
        String sql = "SELECT * FROM otp_codes WHERE operation_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, operationId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapOtpCode(rs);
            }
        } catch (SQLException e) {
            log.error("Error finding OTP code: {}", e.getMessage());
        }
        return null;
    }

    public boolean updateStatus(int otpCodeId, OtpStatus status) {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            stmt.setInt(2, otpCodeId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating OTP status: {}", e.getMessage());
            return false;
        }
    }

    public List<OtpCode> findExpiredActiveCodes() {
        List<OtpCode> expiredCodes = new ArrayList<>();
        String sql = "SELECT * FROM otp_codes WHERE status = 'ACTIVE' AND expires_at < ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                expiredCodes.add(mapOtpCode(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding expired codes: {}", e.getMessage());
        }
        return expiredCodes;
    }

    public boolean markCodesAsExpired(List<Integer> codeIds) {
        if (codeIds.isEmpty()) return true;
        
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' WHERE id = ANY(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            Array array = conn.createArrayOf("integer", codeIds.toArray());
            stmt.setArray(1, array);
            int updated = stmt.executeUpdate();
            log.info("Marked {} codes as expired", updated);
            return true;
        } catch (SQLException e) {
            log.error("Error marking codes as expired: {}", e.getMessage());
            return false;
        }
    }

    public boolean deleteCodesByUserId(int userId) {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            int deleted = stmt.executeUpdate();
            log.info("Deleted {} OTP codes for user id: {}", deleted, userId);
            return true;
        } catch (SQLException e) {
            log.error("Error deleting user codes: {}", e.getMessage());
            return false;
        }
    }

    private OtpCode mapOtpCode(ResultSet rs) throws SQLException {
        OtpCode otpCode = new OtpCode();
        otpCode.setId(rs.getInt("id"));
        otpCode.setOperationId(rs.getString("operation_id"));
        otpCode.setUserId(rs.getInt("user_id"));
        otpCode.setCode(rs.getString("code"));
        otpCode.setStatus(OtpStatus.valueOf(rs.getString("status")));
        
        if (rs.getTimestamp("created_at") != null) {
            otpCode.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("expires_at") != null) {
            otpCode.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        }
        if (rs.getString("delivery_method") != null) {
            otpCode.setDeliveryMethod(DeliveryMethod.valueOf(rs.getString("delivery_method")));
        }
        return otpCode;
    }
}