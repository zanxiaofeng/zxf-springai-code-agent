package com.codeinsight.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-algorithm");
        properties.setExpirationMs(3600000);
        tokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String token = tokenProvider.generateToken("user-123", "testuser", "DEVELOPER");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo("user-123");
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("testuser");
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("DEVELOPER");
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThat(tokenProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    void shouldRejectNullToken() {
        assertThat(tokenProvider.validateToken(null)).isFalse();
    }

    @Test
    void shouldRejectEmptyToken() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    void shouldReturnCorrectExpiration() {
        assertThat(tokenProvider.getExpirationMs()).isEqualTo(3600000);
    }

    @Test
    void shouldGenerateUniqueTokens() {
        String token1 = tokenProvider.generateToken("user-1", "user1", "ADMIN");
        String token2 = tokenProvider.generateToken("user-2", "user2", "DEVELOPER");

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void shouldParseRoleCorrectly() {
        String token = tokenProvider.generateToken("admin-1", "admin", "ADMIN");
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ADMIN");
    }
}
