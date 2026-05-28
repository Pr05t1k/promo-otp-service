package com.promo.otp.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileNotifier {
    private static final Logger log = LoggerFactory.getLogger(FileNotifier.class);
    private static final String OTP_FILE_DIRECTORY = "otp_codes";
    
    static {
        try {
            Path dirPath = Paths.get(OTP_FILE_DIRECTORY);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("Created OTP codes directory: {}", OTP_FILE_DIRECTORY);
            }
        } catch (IOException e) {
            log.error("Failed to create OTP codes directory: {}", e.getMessage());
        }
    }

    public static void saveCodeToFile(String username, String code) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = String.format("%s/otp_%s_%s.txt", OTP_FILE_DIRECTORY, username, timestamp);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("=== OTP Code ===");
            writer.println("User: " + username);
            writer.println("Code: " + code);
            writer.println("Generated: " + LocalDateTime.now());
            writer.println("Valid for: 5 minutes");
            writer.println("===============");
            
            log.info("OTP code saved to file: {}", filename);
        } catch (IOException e) {
            log.error("Failed to save OTP code to file: {}", e.getMessage());
        }
    }
}