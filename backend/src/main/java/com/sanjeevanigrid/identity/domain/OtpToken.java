package com.sanjeevanigrid.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_tokens")
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected OtpToken() {}

    public static OtpToken create(String phone, String code, int expiryMinutes) {
        OtpToken otp = new OtpToken();
        otp.phone = phone;
        otp.code = code;
        otp.expiresAt = Instant.now().plusSeconds(expiryMinutes * 60L);
        return otp;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean matches(String code) {
        return this.code.equals(code);
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markVerified() {
        this.verified = true;
    }

    // Getters
    public UUID getId() { return id; }
    public String getPhone() { return phone; }
    public String getCode() { return code; }
    public int getAttempts() { return attempts; }
    public boolean isVerified() { return verified; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
