package com.sanjeevanigrid.integration.domain;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Service for generating, hashing, and verifying hospital API keys.
 * Raw API keys are prefixed with 'sg_live_' and shown only once upon hospital verification.
 * Only the SHA-256 hash is persisted in the database.
 */
@Service
public class ApiKeyService {

    private static final String KEY_PREFIX = "sg_live_";
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a new cryptographically secure raw API key.
     */
    public String generateRawApiKey() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return KEY_PREFIX + HexFormat.of().formatHex(randomBytes);
    }

    /**
     * Computes the SHA-256 hash of a raw API key.
     */
    public String hashApiKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawKey.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest algorithm not available", e);
        }
    }

    /**
     * Validates whether a raw API key matches a stored SHA-256 hash using constant-time comparison.
     */
    public boolean verifyApiKey(String rawKey, String storedHash) {
        if (rawKey == null || storedHash == null || rawKey.isBlank() || storedHash.isBlank()) {
            return false;
        }
        String calculatedHash = hashApiKey(rawKey);
        return MessageDigest.isEqual(
                calculatedHash.getBytes(StandardCharsets.UTF_8),
                storedHash.trim().getBytes(StandardCharsets.UTF_8)
        );
    }
}
