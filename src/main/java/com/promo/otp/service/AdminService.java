package com.promo.otp.service;

import com.promo.otp.dao.OtpCodeDao;
import com.promo.otp.dao.OtpConfigDao;
import com.promo.otp.dao.UserDao;
import com.promo.otp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminService {
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final UserDao userDao = new UserDao();
    private final OtpConfigDao otpConfigDao = new OtpConfigDao();
    private final OtpCodeDao otpCodeDao = new OtpCodeDao();

    public boolean updateOtpConfig(int codeLength, int ttlSeconds) {
        if (codeLength < 4 || codeLength > 10) {
            log.warn("Invalid code length: {}", codeLength);
            return false;
        }
        if (ttlSeconds < 30 || ttlSeconds > 3600) {
            log.warn("Invalid TTL seconds: {}", ttlSeconds);
            return false;
        }
        
        return otpConfigDao.updateConfig(codeLength, ttlSeconds);
    }

    public List<User> getAllUsers() {
        return userDao.getAllNonAdminUsers();
    }

    public boolean deleteUser(int userId) {
        // First delete all OTP codes associated with the user
        otpCodeDao.deleteCodesByUserId(userId);
        // Then delete the user
        return userDao.deleteUser(userId);
    }
}