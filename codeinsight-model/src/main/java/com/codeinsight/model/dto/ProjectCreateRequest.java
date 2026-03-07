package com.codeinsight.model.dto;

import com.codeinsight.model.enums.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull SourceType sourceType,
        String gitUrl,
        String gitBranch
) {
}
