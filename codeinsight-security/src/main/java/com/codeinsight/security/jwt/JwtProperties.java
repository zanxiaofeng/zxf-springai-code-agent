package com.codeinsight.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "codeinsight.jwt")
public class JwtProperties {

    private String secret = "default-secret-key-must-be-changed-in-production-at-least-256-bits";
    private long expirationMs = 86400000; // 24 hours
}
