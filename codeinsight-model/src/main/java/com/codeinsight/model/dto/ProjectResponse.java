package com.codeinsight.model.dto;

import com.codeinsight.model.enums.IndexStatus;
import com.codeinsight.model.enums.SourceType;

import java.time.LocalDateTime;

public record ProjectResponse(
        String id,
        String name,
        String description,
        SourceType sourceType,
        String gitUrl,
        String gitBranch,
        IndexStatus indexStatus,
        Integer totalFiles,
        Integer totalLines,
        Integer indexedChunks,
        LocalDateTime lastSyncAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
