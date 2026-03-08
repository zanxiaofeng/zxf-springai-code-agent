package com.codeinsight.service;

import com.codeinsight.common.exception.BusinessException;
import com.codeinsight.common.exception.ResourceNotFoundException;
import com.codeinsight.model.dto.ProjectCreateRequest;
import com.codeinsight.model.dto.ProjectResponse;
import com.codeinsight.model.entity.Project;
import com.codeinsight.model.entity.User;
import com.codeinsight.model.enums.IndexStatus;
import com.codeinsight.model.enums.SourceType;
import com.codeinsight.model.repository.ProjectRepository;
import com.codeinsight.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request, String userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .sourceType(request.sourceType())
                .gitUrl(request.gitUrl())
                .gitBranch(org.apache.commons.lang3.ObjectUtils.defaultIfNull(request.gitBranch(), "main"))
                .indexStatus(IndexStatus.PENDING)
                .owner(owner)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Project created: id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(String id, String userId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        if (!project.getOwner().getId().equals(userId)) {
            throw new BusinessException("Access denied to project: " + id);
        }
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listProjects(String userId, Pageable pageable) {
        return projectRepository.findByOwnerId(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void deleteProject(String id, String userId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        if (!project.getOwner().getId().equals(userId)) {
            throw new BusinessException("Access denied to project: " + id);
        }
        projectRepository.deleteById(id);
        log.info("Project deleted: id={}", id);
    }

    @Transactional
    public void updateIndexStatus(String projectId, IndexStatus status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        project.setIndexStatus(status);
        projectRepository.save(project);
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getSourceType(),
                project.getGitUrl(),
                project.getGitBranch(),
                project.getIndexStatus(),
                project.getTotalFiles(),
                project.getTotalLines(),
                project.getIndexedChunks(),
                project.getLastSyncAt(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
