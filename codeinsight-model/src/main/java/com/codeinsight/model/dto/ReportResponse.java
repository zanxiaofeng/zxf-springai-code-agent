package com.codeinsight.model.dto;

import com.codeinsight.model.enums.ReportStatus;
import com.codeinsight.model.enums.ReportType;

import java.time.LocalDateTime;
import java.util.Map;

public record ReportResponse(
        String id,
        String projectId,
        ReportType reportType,
        ReportStatus status,
        Map<String, Object> resultData,
        String summary,
        Integer issueCount,
        Integer criticalCount,
        Integer highCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
