package com.promo.otp.notification;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TelegramNotifier {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    
    private static final String BOT_TOKEN;
    private static final String TELEGRAM_API_URL;

    static {
        BOT_TOKEN = dotenv.get("TELEGRAM_BOT_TOKEN", "");
        TELEGRAM_API_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
    }

    public static void sendCode(String chatId, String code) {
        if (BOT_TOKEN.isEmpty()) {
            log.warn("Telegram bot token not configured. Message not sent.");
            log.info("Would have sent OTP {} to chatId: {}", code, chatId);
            return;
        }
        
        String message = String.format("🔐 *Your OTP Verification Code* 🔐\n\n" +
                                       "Code: `%s`\n\n" +
                                       "This code will expire in 5 minutes.\n" +
                                       "Please do not share this code with anyone.", code);
        
        String url = String.format("%s?chat_id=%s&text=%s&parse_mode=Markdown",
                TELEGRAM_API_URL,
                chatId,
                urlEncode(message));
        
        sendTelegramRequest(url);
    }

    private static void sendTelegramRequest(String url) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                log.info("Telegram message sent successfully");
            } else {
                log.error("Telegram API error. Status code: {}, Response: {}", 
                         response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error sending Telegram message: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}