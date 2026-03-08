package com.codeinsight.model.dto;

import com.codeinsight.model.enums.ScenarioType;

import java.time.LocalDateTime;

public record ConversationResponse(
        String id,
        String title,
        String projectId,
        ScenarioType scenarioType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
