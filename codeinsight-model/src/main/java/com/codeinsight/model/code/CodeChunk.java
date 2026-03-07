package com.codeinsight.model.code;

import com.codeinsight.model.enums.ChunkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeChunk {

    private String content;
    private String filePath;
    private String className;
    private String methodName;
    private int startLine;
    private int endLine;
    private ChunkType chunkType;
    private Map<String, Object> metadata;
}
