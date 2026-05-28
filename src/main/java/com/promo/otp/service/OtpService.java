package com.promo.otp.service;

import com.promo.otp.dao.OtpCodeDao;
import com.promo.otp.dao.OtpConfigDao;
import com.promo.otp.dao.UserDao;
import com.promo.otp.model.*;
import com.promo.otp.notification.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;

public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final OtpCodeDao otpCodeDao = new OtpCodeDao();
    private final OtpConfigDao otpConfigDao = new OtpConfigDao();
    private final UserDao userDao = new UserDao();
    private final SecureRandom random = new SecureRandom();

    public String generateOtpCode(String operationId, int userId, DeliveryMethod method) {
        User user = userDao.findById(userId);
        if (user == null) {
            log.error("User not found with id: {}", userId);
            return null;
        }
        
        OtpConfig config = otpConfigDao.getConfig();
        String code = generateRandomCode(config.getCodeLength());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(config.getTtlSeconds());
        
        OtpCode otpCode = new OtpCode(operationId, userId, code, expiresAt, method);
        
        if (!otpCodeDao.saveOtpCode(otpCode)) {
            log.error("Failed to save OTP code for operation: {}", operationId);
            return null;
        }
        
        // Send code via selected method
        boolean sent = sendCode(user, code, method);
        
        if (sent) {
            log.info("OTP code generated and sent for operation: {} via {}", operationId, method);
            return code;
        } else {
            log.error("Failed to send OTP code for operation: {}", operationId);
            return null;
        }
    }

    public boolean validateOtpCode(String operationId, int userId, String inputCode) {
        OtpCode otpCode = otpCodeDao.findByOperationIdAndUserId(operationId, userId);
        
        if (otpCode == null) {
            log.warn("No OTP code found for operation: {} and user: {}", operationId, userId);
            return false;
        }
        
        if (otpCode.getStatus() != OtpStatus.ACTIVE) {
            log.warn("OTP code is not active. Status: {} for operation: {}", otpCode.getStatus(), operationId);
            return false;
        }
        
        if (otpCode.isExpired()) {
            otpCodeDao.updateStatus(otpCode.getId(), OtpStatus.EXPIRED);
            log.warn("OTP code expired for operation: {}", operationId);
            return false;
        }
        
        if (!otpCode.getCode().equals(inputCode)) {
            log.warn("Invalid OTP code provided for operation: {}", operationId);
            return false;
        }
        
        // Code is valid - mark as used
        otpCodeDao.updateStatus(otpCode.getId(), OtpStatus.USED);
        log.info("OTP code validated successfully for operation: {}", operationId);
        return true;
    }

    private String generateRandomCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private boolean sendCode(User user, String code, DeliveryMethod method) {
        switch (method) {
            case EMAIL:
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    EmailNotifier.sendCode(user.getEmail(), code);
                    return true;
                }
                log.error("User has no email configured: {}", user.getLogin());
                return false;
                
            case SMS:
                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    SmsNotifier.sendCode(user.getPhone(), code);
                    return true;
                }
                log.error("User has no phone configured: {}", user.getLogin());
                return false;
                
            case TELEGRAM:
                if (user.getTelegramChatId() != null && !user.getTelegramChatId().isEmpty()) {
                    TelegramNotifier.sendCode(user.getTelegramChatId(), code);
                    return true;
                }
                log.error("User has no telegram chat ID configured: {}", user.getLogin());
                return false;
                
            case FILE:
                FileNotifier.saveCodeToFile(user.getLogin(), code);
                return true;
                
            default:
                log.error("Unknown delivery method: {}", method);
                return false;
        }
    }
}