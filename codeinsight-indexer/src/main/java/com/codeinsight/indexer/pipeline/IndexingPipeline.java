package com.codeinsight.indexer.pipeline;

import com.codeinsight.indexer.embedding.CodeEmbeddingService;
import com.codeinsight.model.code.CodeChunk;
import com.codeinsight.model.code.ParsedClass;
import com.codeinsight.model.enums.TaskType;
import com.codeinsight.parser.ast.JavaASTParser;
import com.codeinsight.parser.chunker.JavaCodeChunker;
import com.codeinsight.parser.scanner.JavaFileScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingPipeline {

    private final JavaFileScanner fileScanner;
    private final JavaASTParser astParser;
    private final JavaCodeChunker chunker;
    private final CodeEmbeddingService embeddingService;

    @Value("${codeinsight.repo.base-path:./data/repos}")
    private String basePath;

    public void execute(String projectId, TaskType taskType, BiConsumer<Integer, String> progressCallback) throws IOException {
        Path repoPath = Path.of(basePath, projectId);

        progressCallback.accept(5, "Scanning Java files...");
        List<Path> javaFiles = fileScanner.scan(repoPath);
        int totalFiles = javaFiles.size();
        log.info("Found {} Java files for project {}", totalFiles, projectId);

        progressCallback.accept(10, "Parsing " + totalFiles + " Java files...");
        List<CodeChunk> allChunks = new ArrayList<>();
        int processed = 0;

        for (Path file : javaFiles) {
            Optional<ParsedClass> parsedClass = astParser.parse(file);
            if (parsedClass.isPresent()) {
                String relativePath = repoPath.relativize(file).toString();
                String source;
                try {
                    source = Files.readString(file);
                } catch (IOException e) {
                    log.warn("Failed to read file: {}", file, e);
                    continue;
                }
                List<CodeChunk> chunks = chunker.chunkJavaFile(relativePath, source, parsedClass.get());
                allChunks.addAll(chunks);
            }

            processed++;
            int percent = 10 + (int) ((processed / (double) totalFiles) * 60);
            if (processed % 50 == 0) {
                progressCallback.accept(percent, "Parsed " + processed + "/" + totalFiles + " files");
            }
        }

        progressCallback.accept(75, "Embedding " + allChunks.size() + " code chunks...");
        embeddingService.embedAndStore(allChunks, projectId);

        progressCallback.accept(95, "Finalizing index...");
        log.info("Indexing complete for project {}: {} files, {} chunks", projectId, totalFiles, allChunks.size());
    }
}
