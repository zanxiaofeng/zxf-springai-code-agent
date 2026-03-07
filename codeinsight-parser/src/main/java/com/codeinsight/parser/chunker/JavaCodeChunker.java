package com.codeinsight.parser.chunker;

import com.codeinsight.common.constant.AppConstants;
import com.codeinsight.model.code.CodeChunk;
import com.codeinsight.model.code.ParsedClass;
import com.codeinsight.model.code.ParsedMethod;
import com.codeinsight.model.enums.ChunkType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaCodeChunker {

    private final TokenEstimator tokenEstimator;

    public List<CodeChunk> chunkJavaFile(String filePath, String sourceCode, ParsedClass parsedClass) {
        List<CodeChunk> chunks = new ArrayList<>();

        chunks.add(buildClassLevelChunk(filePath, parsedClass));

        for (ParsedMethod method : parsedClass.getMethods()) {
            String methodSource = method.getSourceCode();
            if (tokenEstimator.estimate(methodSource) <= AppConstants.MAX_CHUNK_TOKENS) {
                chunks.add(buildMethodChunk(filePath, parsedClass, method, methodSource));
            } else {
                chunks.addAll(splitLongMethod(filePath, parsedClass, method));
            }
        }

        return chunks;
    }

    private CodeChunk buildClassLevelChunk(String filePath, ParsedClass parsedClass) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(parsedClass.getPackageName()).append(";\n\n");
        if (parsedClass.getAnnotations() != null) {
            parsedClass.getAnnotations().forEach(a -> sb.append("@").append(a).append("\n"));
        }
        sb.append("class ").append(parsedClass.getClassName());
        if (parsedClass.getSuperClass() != null) {
            sb.append(" extends ").append(parsedClass.getSuperClass());
        }
        if (parsedClass.getImplementedInterfaces() != null && !parsedClass.getImplementedInterfaces().isEmpty()) {
            sb.append(" implements ").append(String.join(", ", parsedClass.getImplementedInterfaces()));
        }
        sb.append(" {\n");
        if (parsedClass.getFields() != null) {
            parsedClass.getFields().forEach(f ->
                    sb.append("  ").append(f.getType()).append(" ").append(f.getName()).append(";\n"));
        }
        sb.append("}");

        return CodeChunk.builder()
                .content(sb.toString())
                .filePath(filePath)
                .className(parsedClass.getQualifiedName())
                .chunkType(ChunkType.CLASS)
                .startLine(1)
                .endLine(1)
                .metadata(Map.of(
                        "packageName", parsedClass.getPackageName(),
                        "classType", parsedClass.getClassType()
                ))
                .build();
    }

    private CodeChunk buildMethodChunk(String filePath, ParsedClass cls, ParsedMethod method, String source) {
        return CodeChunk.builder()
                .content(source)
                .filePath(filePath)
                .className(cls.getQualifiedName())
                .methodName(method.getName())
                .startLine(method.getStartLine())
                .endLine(method.getEndLine())
                .chunkType(ChunkType.METHOD)
                .metadata(Map.of(
                        "annotations", String.join(",", method.getAnnotations()),
                        "returnType", method.getReturnType(),
                        "complexity", method.getComplexity()
                ))
                .build();
    }

    private List<CodeChunk> splitLongMethod(String filePath, ParsedClass cls, ParsedMethod method) {
        List<CodeChunk> chunks = new ArrayList<>();
        String[] lines = method.getSourceCode().split("\n");
        int maxLinesPerChunk = 40;

        for (int i = 0; i < lines.length; i += maxLinesPerChunk - AppConstants.OVERLAP_LINES) {
            int end = Math.min(i + maxLinesPerChunk, lines.length);
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < end; j++) {
                sb.append(lines[j]).append("\n");
            }

            chunks.add(CodeChunk.builder()
                    .content(sb.toString())
                    .filePath(filePath)
                    .className(cls.getQualifiedName())
                    .methodName(method.getName())
                    .startLine(method.getStartLine() + i)
                    .endLine(method.getStartLine() + end - 1)
                    .chunkType(ChunkType.METHOD_PART)
                    .metadata(Map.of(
                            "partIndex", chunks.size(),
                            "annotations", String.join(",", method.getAnnotations()),
                            "returnType", method.getReturnType()
                    ))
                    .build());

            if (end >= lines.length) {
                break;
            }
        }

        return chunks;
    }
}
