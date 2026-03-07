package com.codeinsight.parser;

import com.codeinsight.model.enums.SourceType;
import com.codeinsight.parser.git.ArchiveExtractService;
import com.codeinsight.parser.git.GitCloneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeSourceResolver {

    private final GitCloneService gitCloneService;
    private final ArchiveExtractService archiveExtractService;

    public Path resolve(SourceType sourceType, String projectId, String gitUrl, String branch) throws IOException, GitAPIException {
        return switch (sourceType) {
            case GIT -> gitCloneService.cloneRepository(gitUrl, branch, projectId);
            case ARCHIVE -> gitCloneService.getRepoPath(projectId);
        };
    }
}
