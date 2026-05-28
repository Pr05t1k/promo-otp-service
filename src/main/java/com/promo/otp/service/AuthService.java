package com.promo.otp.service;

import com.promo.otp.dao.UserDao;
import com.promo.otp.model.Role;
import com.promo.otp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserDao userDao = new UserDao();

    public boolean register(String login, String password, String role, 
                            String email, String phone, String telegramChatId) {
        
        // Check if trying to create admin and admin already exists
        if ("ADMIN".equalsIgnoreCase(role)) {
            if (userDao.adminExists()) {
                log.warn("Attempt to create second admin user: {}", login);
                return false;
            }
        }
        
        // Check if user already exists
        if (userDao.findByLogin(login) != null) {
            log.warn("User already exists: {}", login);
            return false;
        }
        
        // Create new user
        User newUser = new User();
        newUser.setLogin(login);
        newUser.setRole(Role.valueOf(role.toUpperCase()));
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setTelegramChatId(telegramChatId);
        
        return userDao.createUser(newUser, password);
    }

    public User login(String login, String password) {
        User user = userDao.findByLogin(login);
        
        if (user == null) {
            log.warn("Login failed - user not found: {}", login);
            return null;
        }
        
        if (!userDao.checkPassword(user, password)) {
            log.warn("Login failed - invalid password for user: {}", login);
            return null;
        }
        
        log.info("User logged in successfully: {}", login);
        return user;
    }
    public User getUserById(int id) {
    return userDao.findById(id);
}
}