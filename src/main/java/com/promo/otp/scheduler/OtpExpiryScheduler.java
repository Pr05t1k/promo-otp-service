package com.promo.otp.scheduler;

import com.promo.otp.dao.OtpCodeDao;
import com.promo.otp.model.OtpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OtpExpiryScheduler {
    private static final Logger log = LoggerFactory.getLogger(OtpExpiryScheduler.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final OtpCodeDao otpCodeDao = new OtpCodeDao();

    public void start() {
        // Run every minute to check for expired codes
        scheduler.scheduleAtFixedRate(this::markExpiredCodes, 0, 1, TimeUnit.MINUTES);
        log.info("OTP expiry scheduler started - checking every minute");
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info("OTP expiry scheduler stopped");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void markExpiredCodes() {
        try {
            List<OtpCode> expiredCodes = otpCodeDao.findExpiredActiveCodes();
            
            if (!expiredCodes.isEmpty()) {
                List<Integer> expiredIds = expiredCodes.stream()
                        .map(OtpCode::getId)
                        .collect(Collectors.toList());
                
                otpCodeDao.markCodesAsExpired(expiredIds);
                log.info("Marked {} OTP codes as expired", expiredIds.size());
            }
        } catch (Exception e) {
            log.error("Error in expiry scheduler: {}", e.getMessage());
        }
    }
}