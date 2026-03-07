package com.codeinsight.model.dto;

import com.codeinsight.model.enums.ScenarioType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatRequest(
        @NotNull String projectId,
        String conversationId,
        @NotBlank String message,
        ScenarioType scenario
) {
}
