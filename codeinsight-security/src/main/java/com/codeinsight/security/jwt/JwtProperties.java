package com.codeinsight.security.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;

@Data
@Slf4j
@ConfigurationProperties(prefix = "codeinsight.jwt")
public class JwtProperties {

    private String secret = "default-secret-key-must-be-changed-in-production-at-least-256-bits";
    private long expirationMs = 86400000; // 24 hours

    @PostConstruct
    void validateSecret() {
        Assert.hasText(secret, "JWT secret must be configured via JWT_SECRET or codeinsight.jwt.secret");
        if (secret.startsWith("default-secret") || secret.contains("codeinsight-dev")) {
            log.warn("Using default JWT secret — set JWT_SECRET environment variable for production!");
        }
        Assert.isTrue(secret.getBytes(StandardCharsets.UTF_8).length >= 32,
                "JWT secret must be at least 256 bits (32 bytes)");
    }
}
