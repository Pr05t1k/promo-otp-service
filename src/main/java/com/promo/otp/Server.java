package com.promo.otp;

import com.promo.otp.controller.AdminController;
import com.promo.otp.controller.AuthController;
import com.promo.otp.controller.UserController;
import com.promo.otp.scheduler.OtpExpiryScheduler;
import com.promo.otp.security.AuthFilter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private static final int PORT = 8080;
    private static HttpServer server;
    private static OtpExpiryScheduler scheduler;

    public static void main(String[] args) {
        try {
            // Create HTTP server
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Create controller instances
            AuthController authController = new AuthController();
            UserController userController = new UserController();
            AdminController adminController = new AdminController();

            // Public routes (no authentication required)
            server.createContext("/api/auth/register", authController);
            server.createContext("/api/auth/login", authController);

            // Protected user routes with authentication
            server.createContext("/api/user", new HttpHandler() {
                private final AuthFilter authFilter = new AuthFilter("USER");

                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (authFilter.authenticate(exchange)) {
                        userController.handle(exchange);
                    }
                }
            });

            // Protected admin routes with authentication
            server.createContext("/api/admin", new HttpHandler() {
                private final AuthFilter authFilter = new AuthFilter("ADMIN");

                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (authFilter.authenticate(exchange)) {
                        adminController.handle(exchange);
                    }
                }
            });

            // Configure thread pool
            server.setExecutor(Executors.newFixedThreadPool(10));

            // Start the server
            server.start();

            // Start the expiry scheduler
            scheduler = new OtpExpiryScheduler();
            scheduler.start();

            log.info("=".repeat(60));
            log.info("Promo OTP Service started successfully!");
            log.info("Server listening on port: {}", PORT);
            log.info("API endpoints:");
            log.info("  POST   /api/auth/register  - Register new user");
            log.info("  POST   /api/auth/login     - Login and get JWT token");
            log.info("  POST   /api/user/otp/generate - Generate OTP code (auth required)");
            log.info("  POST   /api/user/otp/validate - Validate OTP code (auth required)");
            log.info("  GET    /api/admin/users    - List all users (admin only)");
            log.info("  DELETE /api/admin/users/{id} - Delete user (admin only)");
            log.info("  PUT    /api/admin/config   - Update OTP config (admin only)");
            log.info("=".repeat(60));

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down server...");
                if (server != null) {
                    server.stop(5);
                }
                if (scheduler != null) {
                    scheduler.stop();
                }
                log.info("Server stopped");
            }));

        } catch (IOException e) {
            log.error("Failed to start server", e);
            System.exit(1);
        }
    }
}