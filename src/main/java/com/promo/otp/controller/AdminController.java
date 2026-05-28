package com.promo.otp.controller;

import com.promo.otp.model.User;
import com.promo.otp.service.AdminService;
import com.promo.otp.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminController implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService = new AdminService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        String login = (String) exchange.getAttribute("login");
        
        try {
            if (path.equals("/api/admin/config") && "PUT".equals(method)) {
                handleUpdateConfig(exchange, login);
            } else if (path.equals("/api/admin/users") && "GET".equals(method)) {
                handleGetUsers(exchange, login);
            } else if (path.matches("/api/admin/users/\\d+") && "DELETE".equals(method)) {
                handleDeleteUser(exchange, login);
            } else {
                sendResponse(exchange, 404, Map.of("error", "Endpoint not found"));
            }
        } catch (Exception e) {
            log.error("Error handling admin request", e);
            sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleUpdateConfig(HttpExchange exchange, String adminLogin) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Integer> request = JsonUtil.fromJson(body, Map.class);
        
        if (request == null) {
            sendResponse(exchange, 400, Map.of("error", "Invalid JSON body"));
            return;
        }
        
        Integer codeLength = request.get("codeLength");
        Integer ttlSeconds = request.get("ttlSeconds");
        
        if (codeLength == null && ttlSeconds == null) {
            sendResponse(exchange, 400, Map.of("error", "At least one of codeLength or ttlSeconds is required"));
            return;
        }
        
        // Get current config first
        com.promo.otp.dao.OtpConfigDao configDao = new com.promo.otp.dao.OtpConfigDao();
        com.promo.otp.model.OtpConfig currentConfig = configDao.getConfig();
        
        int newCodeLength = codeLength != null ? codeLength : currentConfig.getCodeLength();
        int newTtlSeconds = ttlSeconds != null ? ttlSeconds : currentConfig.getTtlSeconds();
        
        boolean success = adminService.updateOtpConfig(newCodeLength, newTtlSeconds);
        
        if (success) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "OTP configuration updated successfully");
            response.put("codeLength", newCodeLength);
            response.put("ttlSeconds", newTtlSeconds);
            
            log.info("Admin {} updated OTP config: codeLength={}, ttlSeconds={}", 
                     adminLogin, newCodeLength, newTtlSeconds);
            sendResponse(exchange, 200, response);
        } else {
            sendResponse(exchange, 400, Map.of("error", "Invalid configuration values. codeLength must be 4-10, ttlSeconds must be 30-3600"));
        }
    }

    private void handleGetUsers(HttpExchange exchange, String adminLogin) throws IOException {
        List<User> users = adminService.getAllUsers();
        
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("login", user.getLogin());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("hasTelegram", user.getTelegramChatId() != null && !user.getTelegramChatId().isEmpty());
            userMap.put("createdAt", user.getCreatedAt());
            return userMap;
        }).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", userList);
        response.put("count", userList.size());
        
        log.info("Admin {} retrieved list of {} users", adminLogin, userList.size());
        sendResponse(exchange, 200, response);
    }

    private void handleDeleteUser(HttpExchange exchange, String adminLogin) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        int userId = Integer.parseInt(parts[parts.length - 1]);
        
        boolean success = adminService.deleteUser(userId);
        
        if (success) {
            Map<String, String> response = Map.of("message", "User deleted successfully");
            log.info("Admin {} deleted user with id: {}", adminLogin, userId);
            sendResponse(exchange, 200, response);
        } else {
            sendResponse(exchange, 404, Map.of("error", "User not found or cannot be deleted (only non-admin users can be deleted)"));
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, Object responseBody) throws IOException {
        String response = JsonUtil.toJson(responseBody);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
        
        log.info("Response: {} {}", statusCode, response);
    }
}