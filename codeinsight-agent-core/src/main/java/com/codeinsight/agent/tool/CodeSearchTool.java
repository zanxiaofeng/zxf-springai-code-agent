package com.codeinsight.agent.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeSearchTool {

    private final VectorStore vectorStore;

    @Tool(description = "Search for code snippets semantically related to the query in the project codebase. Returns relevant code with file path and line numbers.")
    public String searchCode(
            @ToolParam(description = "Search query describing the code to find") String query,
            @ToolParam(description = "Project ID") String projectId,
            @ToolParam(description = "Number of results, default 5") int topK) {

        int k = topK > 0 ? topK : 5;
        log.debug("searchCode: query='{}', projectId={}, topK={}", query, projectId, k);

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(0.6)
                .filterExpression(new FilterExpressionBuilder().eq("projectId", projectId).build())
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        log.debug("searchCode: found {} results for query='{}'", results.size(), query);

        if (results.isEmpty()) {
            return "No relevant code found for query: " + query;
        }

        return results.stream()
                .map(doc -> {
                    String filePath = String.valueOf(doc.getMetadata().get("filePath"));
                    String className = String.valueOf(doc.getMetadata().get("className"));
                    String methodName = String.valueOf(doc.getMetadata().getOrDefault("methodName", ""));
                    int startLine = Integer.parseInt(String.valueOf(doc.getMetadata().getOrDefault("startLine", "0")));
                    log.debug("searchCode result: {}:{} ({}.{}) - score={}", filePath, startLine, className, methodName, doc.getMetadata().get("distance"));

                    return String.format("### %s:%d (%s.%s)\n```java\n%s\n```",
                            filePath, startLine, className, methodName, doc.getText());
                })
                .collect(Collectors.joining("\n\n"));
    }
}
