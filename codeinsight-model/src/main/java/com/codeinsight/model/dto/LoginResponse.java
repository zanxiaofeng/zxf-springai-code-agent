package com.codeinsight.model.dto;

public record LoginResponse(
        String accessToken,
        long expiresIn,
        String username,
        String role
) {
}
