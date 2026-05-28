package com.promo.otp.notification;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EmailNotifier {
    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    
    private static String username;
    private static String password;
    private static String fromEmail;
    private static Session session;

    static {
        username = dotenv.get("EMAIL_USERNAME", "");
        password = dotenv.get("EMAIL_PASSWORD", "");
        fromEmail = dotenv.get("EMAIL_FROM", username);
        
        Properties props = new Properties();
        props.put("mail.smtp.host", dotenv.get("SMTP_HOST", "smtp.gmail.com"));
        props.put("mail.smtp.port", dotenv.get("SMTP_PORT", "587"));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    public static void sendCode(String toEmail, String code) {
        if (username.isEmpty() || password.isEmpty()) {
            log.warn("Email credentials not configured. Email not sent.");
            log.info("Would have sent OTP {} to email: {}", code, toEmail);
            return;
        }
        
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Your OTP Verification Code");
            message.setText(String.format("""
                Your OTP verification code is: %s
                
                This code will expire in 5 minutes.
                Please do not share this code with anyone.
                
                Best regards,
                Promo OTP Service
                """, code));
            
            Transport.send(message);
            log.info("OTP code sent to email: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}