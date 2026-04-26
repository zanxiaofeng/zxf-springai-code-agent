package com.codeinsight.indexer.embedding;

import com.codeinsight.model.code.CodeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeEmbeddingService {

    private final VectorStore vectorStore;

    public void embedAndStore(List<CodeChunk> chunks, String projectId) {
        log.info("Starting embedding: {} chunks for project {}", chunks.size(), projectId);
        List<Document> documents = chunks.stream()
                .map(chunk -> toDocument(chunk, projectId))
                .toList();

        int batchSize = 10;
        int totalBatches = (documents.size() + batchSize - 1) / batchSize;
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            vectorStore.add(batch);
            log.debug("Stored batch {}/{} ({}-{} of {} documents)", (i / batchSize) + 1, totalBatches, i, end, documents.size());
        }

        log.info("Embedded and stored {} chunks in {} batches for project {}", documents.size(), totalBatches, projectId);
    }

    private Document toDocument(CodeChunk chunk, String projectId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectId", projectId);
        metadata.put("filePath", chunk.getFilePath());
        metadata.put("className", chunk.getClassName());
        metadata.put("chunkType", chunk.getChunkType().name());
        metadata.put("startLine", chunk.getStartLine());
        metadata.put("endLine", chunk.getEndLine());

        if (chunk.getMethodName() != null) {
            metadata.put("methodName", chunk.getMethodName());
        }
        if (chunk.getMetadata() != null) {
            chunk.getMetadata().forEach((k, v) -> {
                if (v != null) {
                    metadata.put(k, v.toString());
                }
            });
        }

        return new Document(chunk.getContent(), metadata);
    }
}
