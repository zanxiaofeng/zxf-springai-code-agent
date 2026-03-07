package com.codeinsight.model.dto;

import com.codeinsight.model.enums.TaskStatus;
import com.codeinsight.model.enums.TaskType;

import java.time.LocalDateTime;

public record TaskResponse(
        String id,
        TaskType taskType,
        String projectId,
        TaskStatus status,
        Integer progressPercent,
        String progressMessage,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
