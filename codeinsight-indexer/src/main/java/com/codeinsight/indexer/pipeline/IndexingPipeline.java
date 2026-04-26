package com.codeinsight.indexer.pipeline;

import com.codeinsight.indexer.embedding.CodeEmbeddingService;
import com.codeinsight.model.code.CodeChunk;
import com.codeinsight.model.code.ParsedClass;
import com.codeinsight.model.entity.Project;
import com.codeinsight.model.enums.TaskType;
import com.codeinsight.model.repository.ProjectRepository;
import com.codeinsight.parser.CodeSourceResolver;
import com.codeinsight.parser.ast.JavaASTParser;
import com.codeinsight.parser.chunker.JavaCodeChunker;
import com.codeinsight.parser.scanner.JavaFileScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
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
    private final CodeSourceResolver codeSourceResolver;
    private final ProjectRepository projectRepository;

    public void execute(String projectId, TaskType taskType, BiConsumer<Integer, String> progressCallback) throws IOException, GitAPIException {
        log.info("Starting indexing pipeline: projectId={}, taskType={}", projectId, taskType);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IOException("Project not found: " + projectId));

        progressCallback.accept(2, "Resolving source...");
        Path repoPath = codeSourceResolver.resolve(
                project.getSourceType(), projectId, project.getGitUrl(), project.getGitBranch());
        log.info("Resolved source path: {} (sourceType={})", repoPath, project.getSourceType());

        progressCallback.accept(5, "Scanning Java files...");
        List<Path> javaFiles = fileScanner.scan(repoPath);
        int totalFiles = javaFiles.size();
        log.info("Found {} Java files for project {}", totalFiles, projectId);

        progressCallback.accept(10, "Parsing " + totalFiles + " Java files...");
        List<CodeChunk> allChunks = new ArrayList<>();
        int processed = 0;
        int parsedOk = 0;
        int parseFail = 0;

        for (Path file : javaFiles) {
            Optional<ParsedClass> parsedClass = astParser.parse(file);
            if (parsedClass.isPresent()) {
                parsedOk++;
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
            } else {
                parseFail++;
                log.debug("Skipped file (parse failed or not a class): {}", file.getFileName());
            }

            processed++;
            int percent = 10 + (int) ((processed / (double) totalFiles) * 60);
            if (processed % 50 == 0) {
                progressCallback.accept(percent, "Parsed " + processed + "/" + totalFiles + " files");
            }
        }

        log.info("Parsing complete: {} parsed, {} skipped, {} chunks generated", parsedOk, parseFail, allChunks.size());

        progressCallback.accept(75, "Embedding " + allChunks.size() + " code chunks...");
        embeddingService.embedAndStore(allChunks, projectId);

        progressCallback.accept(95, "Finalizing index...");
        log.info("Indexing complete for project {}: {} files, {} chunks", projectId, totalFiles, allChunks.size());
    }
}
