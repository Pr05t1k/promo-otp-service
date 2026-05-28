package com.promo.otp.notification;

import io.github.cdimascio.dotenv.Dotenv;
import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class SmsNotifier {
    private static final Logger log = LoggerFactory.getLogger(SmsNotifier.class);
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private static String host;
    private static int port;
    private static String systemId;
    private static String password;
    private static String systemType;
    private static String sourceAddress;

    static {
        host = dotenv.get("SMPP_HOST", "localhost");
        port = Integer.parseInt(dotenv.get("SMPP_PORT", "2775"));
        systemId = dotenv.get("SMPP_SYSTEM_ID", "smppclient1");
        password = dotenv.get("SMPP_PASSWORD", "password");
        systemType = dotenv.get("SMPP_SYSTEM_TYPE", "OTP");
        sourceAddress = dotenv.get("SMPP_SOURCE_ADDR", "OTPService");
    }

    public static void sendCode(String phoneNumber, String code) {
        // Для тестирования просто логируем, так как эмулятор может быть не запущен
        log.info("=== SMS NOTIFICATION ===");
        log.info("To: {}", phoneNumber);
        log.info("OTP Code: {}", code);
        log.info("Message: Your OTP verification code is: {}", code);
        log.info("========================");

        // Раскомментируй этот блок, когда настроишь реальный SMPP сервер
        /*
        SMPPSession session = new SMPPSession();

        try {
            // Подключение к SMPP серверу
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );

            session.connectAndBind(host, port, bindParameter);
            log.info("Connected to SMPP server at {}:{}", host, port);

            String messageText = "Your OTP code: " + code;

            // Создаем SubmitSm объект
            SubmitSm submitSm = new SubmitSm();
            submitSm.setSourceAddrTon(TypeOfNumber.UNKNOWN);
            submitSm.setSourceAddrNpi(NumberingPlanIndicator.UNKNOWN);
            submitSm.setSourceAddr(sourceAddress);
            submitSm.setDestAddrTon(TypeOfNumber.UNKNOWN);
            submitSm.setDestAddrNpi(NumberingPlanIndicator.UNKNOWN);
            submitSm.setDestAddr(phoneNumber);
            submitSm.setShortMessage(messageText.getBytes(StandardCharsets.UTF_8));
            submitSm.setDataCoding(new GeneralDataCoding(Alphabet.ALPHA_DEFAULT));
            submitSm.setRegisteredDelivery(new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT));
            submitSm.setEsmClass(new ESMClass());

            // Отправка сообщения
            String messageId = session.submitShortMessage(
                    submitSm.getSourceAddrTon(),
                    submitSm.getSourceAddrNpi(),
                    submitSm.getSourceAddr(),
                    submitSm.getDestAddrTon(),
                    submitSm.getDestAddrNpi(),
                    submitSm.getDestAddr(),
                    submitSm.getEsmClass(),
                    submitSm.getProtocolId(),
                    submitSm.getPriorityFlag(),
                    submitSm.getScheduleDeliveryTime(),
                    submitSm.getValidityPeriod(),
                    submitSm.getRegisteredDelivery(),
                    submitSm.getReplaceIfPresentFlag(),
                    submitSm.getDataCoding(),
                    submitSm.getSmDefaultMsgId(),
                    submitSm.getShortMessage()
            );

            log.info("SMS sent successfully. Message ID: {}, Phone: {}", messageId, phoneNumber);
            session.unbindAndClose();

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            log.error("Exception details: ", e);
        } finally {
            if (session != null && session.getSessionState().isBound()) {
                try {
                    session.unbindAndClose();
                } catch (Exception e) {
                    log.warn("Error closing SMPP session: {}", e.getMessage());
                }
            }
        }
        */
    }
}