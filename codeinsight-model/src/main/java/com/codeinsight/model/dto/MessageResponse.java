package com.codeinsight.model.dto;

import com.codeinsight.model.enums.MessageRole;

import java.time.LocalDateTime;

public record MessageResponse(
        String id,
        MessageRole role,
        String content,
        Integer tokenCount,
        LocalDateTime createdAt
) {
}
