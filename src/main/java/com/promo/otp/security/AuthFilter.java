package com.promo.otp.security;

import com.promo.otp.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private final String requiredRole;

    public AuthFilter(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    public boolean authenticate(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(exchange, "Missing or invalid authorization header");
            return false;
        }

        String token = authHeader.substring(7);
        String role = JwtUtil.getRoleFromToken(token);
        String login = JwtUtil.getLoginFromToken(token);
        Integer userId = JwtUtil.getUserIdFromToken(token);

        if (role == null || login == null || userId == null) {
            sendUnauthorized(exchange, "Invalid or expired token");
            return false;
        }

        // Check role authorization
        if (requiredRole != null && !role.equals(requiredRole) && !role.equals("ADMIN")) {
            sendForbidden(exchange, "Insufficient permissions");
            return false;
        }

        // Store user info in exchange attributes for later use
        exchange.setAttribute("userId", userId);
        exchange.setAttribute("login", login);
        exchange.setAttribute("role", role);

        log.info("Authenticated request from user: {} with role: {}", login, role);
        return true;
    }

    private void sendUnauthorized(HttpExchange exchange, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        String response = JsonUtil.toJson(error);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(401, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.getResponseBody().close();
    }

    private void sendForbidden(HttpExchange exchange, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        String response = JsonUtil.toJson(error);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(403, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.getResponseBody().close();
    }
}