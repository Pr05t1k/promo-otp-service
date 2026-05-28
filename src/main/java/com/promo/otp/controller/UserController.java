package com.promo.otp.controller;

import com.promo.otp.model.DeliveryMethod;
import com.promo.otp.service.OtpService;
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

public class UserController implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final OtpService otpService = new OtpService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        // Get user ID from the attribute set by AuthFilter
        Integer userId = (Integer) exchange.getAttribute("userId");
        String login = (String) exchange.getAttribute("login");
        
        if (userId == null) {
            sendResponse(exchange, 401, Map.of("error", "User not authenticated"));
            return;
        }
        
        try {
            if (path.equals("/api/user/otp/generate") && "POST".equals(method)) {
                handleGenerateOtp(exchange, userId, login);
            } else if (path.equals("/api/user/otp/validate") && "POST".equals(method)) {
                handleValidateOtp(exchange, userId, login);
            } else {
                sendResponse(exchange, 404, Map.of("error", "Endpoint not found"));
            }
        } catch (Exception e) {
            log.error("Error handling user request", e);
            sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleGenerateOtp(HttpExchange exchange, int userId, String login) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> request = JsonUtil.fromJson(body, Map.class);
        
        if (request == null) {
            sendResponse(exchange, 400, Map.of("error", "Invalid JSON body"));
            return;
        }
        
        String operationId = request.get("operationId");
        String deliveryMethodStr = request.get("deliveryMethod");
        
        if (operationId == null || operationId.trim().isEmpty()) {
            sendResponse(exchange, 400, Map.of("error", "operationId is required"));
            return;
        }
        
        if (deliveryMethodStr == null || deliveryMethodStr.trim().isEmpty()) {
            sendResponse(exchange, 400, Map.of("error", "deliveryMethod is required (EMAIL, SMS, TELEGRAM, or FILE)"));
            return;
        }
        
        DeliveryMethod deliveryMethod;
        try {
            deliveryMethod = DeliveryMethod.valueOf(deliveryMethodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of("error", "Invalid delivery method. Use: EMAIL, SMS, TELEGRAM, or FILE"));
            return;
        }
        
        String code = otpService.generateOtpCode(operationId, userId, deliveryMethod);
        
        if (code != null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "OTP code generated and sent successfully");
            response.put("operationId", operationId);
            response.put("deliveryMethod", deliveryMethod.name());
            // Don't return the actual code in production! This is for testing only
            response.put("debug_code", code); // For testing purposes only
            
            log.info("OTP generated for user {} operation {} via {}", login, operationId, deliveryMethod);
            sendResponse(exchange, 200, response);
        } else {
            sendResponse(exchange, 500, Map.of("error", "Failed to generate or send OTP code"));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleValidateOtp(HttpExchange exchange, int userId, String login) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> request = JsonUtil.fromJson(body, Map.class);
        
        if (request == null) {
            sendResponse(exchange, 400, Map.of("error", "Invalid JSON body"));
            return;
        }
        
        String operationId = request.get("operationId");
        String code = request.get("code");
        
        if (operationId == null || operationId.trim().isEmpty()) {
            sendResponse(exchange, 400, Map.of("error", "operationId is required"));
            return;
        }
        
        if (code == null || code.trim().isEmpty()) {
            sendResponse(exchange, 400, Map.of("error", "code is required"));
            return;
        }
        
        boolean isValid = otpService.validateOtpCode(operationId, userId, code);
        
        if (isValid) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "OTP code validated successfully");
            response.put("operationId", operationId);
            response.put("status", "VERIFIED");
            
            log.info("OTP validated successfully for user {} operation {}", login, operationId);
            sendResponse(exchange, 200, response);
        } else {
            sendResponse(exchange, 400, Map.of("error", "Invalid or expired OTP code"));
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