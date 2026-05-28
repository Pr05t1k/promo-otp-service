package com.promo.otp.controller;

import com.promo.otp.model.User;
import com.promo.otp.security.JwtUtil;
import com.promo.otp.service.AuthService;
import com.promo.otp.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AuthController implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            if (path.equals("/api/auth/register") && "POST".equals(method)) {
                handleRegister(exchange);
            } else if (path.equals("/api/auth/login") && "POST".equals(method)) {
                handleLogin(exchange);
            } else {
                sendResponse(exchange, 404, Map.of("error", "Endpoint not found"));
            }
        } catch (Exception e) {
            log.error("Error handling auth request", e);
            sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> request = JsonUtil.fromJson(body, Map.class);
        
        if (request == null) {
            sendResponse(exchange, 400, Map.of("error", "Invalid JSON body"));
            return;
        }
        
        String login = request.get("login");
        String password = request.get("password");
        String role = request.getOrDefault("role", "USER");
        String email = request.get("email");
        String phone = request.get("phone");
        String telegramChatId = request.get("telegramChatId");
        
        // Validation
        if (login == null || login.trim().isEmpty()) {
            sendResponse(exchange, 400, Map.of("error", "Login is required"));
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            sendResponse(exchange, 400, Map.of("error", "Password is required"));
            return;
        }
        if (password.length() < 6) {
            sendResponse(exchange, 400, Map.of("error", "Password must be at least 6 characters"));
            return;
        }
        
        boolean success = authService.register(login, password, role, email, phone, telegramChatId);
        
        if (success) {
            log.info("User registered successfully: {}", login);
            sendResponse(exchange, 201, Map.of("message", "User registered successfully"));
        } else {
            sendResponse(exchange, 400, Map.of("error", "Registration failed. Admin may already exist or user already exists"));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> request = JsonUtil.fromJson(body, Map.class);
        
        if (request == null) {
            sendResponse(exchange, 400, Map.of("error", "Invalid JSON body"));
            return;
        }
        
        String login = request.get("login");
        String password = request.get("password");
        
        if (login == null || password == null) {
            sendResponse(exchange, 400, Map.of("error", "Login and password are required"));
            return;
        }
        
        User user = authService.login(login, password);
        
        if (user != null) {
            String token = JwtUtil.generateToken(user.getLogin(), user.getRole().name(), user.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole().name());
            response.put("userId", user.getId());
            response.put("login", user.getLogin());
            
            log.info("User logged in: {}", login);
            sendResponse(exchange, 200, response);
        } else {
            sendResponse(exchange, 401, Map.of("error", "Invalid credentials"));
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, Object responseBody) throws IOException {
        String response = JsonUtil.toJson(responseBody);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
        
        log.info("Response: {} {}", statusCode, response);
    }
}