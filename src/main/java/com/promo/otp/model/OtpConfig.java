package com.promo.otp.model;

import java.time.LocalDateTime;

public class OtpConfig {
    private int id;
    private int codeLength;
    private int ttlSeconds;
    private LocalDateTime updatedAt;

    public OtpConfig() {
        this.codeLength = 6;
        this.ttlSeconds = 300;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCodeLength() { return codeLength; }
    public void setCodeLength(int codeLength) { this.codeLength = codeLength; }

    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}