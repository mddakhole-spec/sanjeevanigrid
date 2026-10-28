package com.sanjeevanigrid.integration.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyServiceTest {

    private final ApiKeyService service = new ApiKeyService();

    @Test
    @DisplayName("Generates API key with sg_live_ prefix and sufficient entropy")
    void testGenerateRawApiKey() {
        String key = service.generateRawApiKey();
        assertThat(key).startsWith("sg_live_");
        assertThat(key.length()).isGreaterThan(30);
    }

    @Test
    @DisplayName("SHA-256 hash generation is deterministic and non-empty")
    void testHashApiKey() {
        String hash1 = service.hashApiKey("sg_live_demo_key_123");
        String hash2 = service.hashApiKey("sg_live_demo_key_123");
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex length
    }

    @Test
    @DisplayName("verifyApiKey succeeds for matching key and fails for mismatched/tampered key")
    void testVerifyApiKey() {
        String rawKey = "sg_live_test_hospital_abc123";
        String hash = service.hashApiKey(rawKey);

        assertThat(service.verifyApiKey(rawKey, hash)).isTrue();
        assertThat(service.verifyApiKey("sg_live_wrong_key", hash)).isFalse();
        assertThat(service.verifyApiKey("", hash)).isFalse();
        assertThat(service.verifyApiKey(null, hash)).isFalse();
        assertThat(service.verifyApiKey(rawKey, null)).isFalse();
    }

    @Test
    @DisplayName("Hashing empty key throws IllegalArgumentException")
    void testEmptyKeyThrows() {
        assertThatThrownBy(() -> service.hashApiKey(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
