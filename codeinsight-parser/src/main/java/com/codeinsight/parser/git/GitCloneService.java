package com.codeinsight.parser.git;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class GitCloneService {

    @Value("${codeinsight.repo.base-path:./data/repos}")
    private String basePath;

    public Path cloneRepository(String gitUrl, String branch, String projectId) throws GitAPIException, IOException {
        Path targetDir = Path.of(basePath, projectId);

        if (Files.exists(targetDir)) {
            log.info("Repository already exists at {}, pulling latest", targetDir);
            try (Git git = Git.open(targetDir.toFile())) {
                git.pull().call();
            }
            return targetDir;
        }

        Files.createDirectories(targetDir.getParent());
        log.info("Cloning {} (branch: {}) to {}", gitUrl, branch, targetDir);

        Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(targetDir.toFile())
                .setBranch(branch)
                .setCloneAllBranches(false)
                .call()
                .close();

        return targetDir;
    }

    public Path getRepoPath(String projectId) {
        return Path.of(basePath, projectId);
    }
}
