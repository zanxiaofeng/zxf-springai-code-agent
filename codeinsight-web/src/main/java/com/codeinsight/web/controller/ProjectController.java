package com.codeinsight.web.controller;

import com.codeinsight.common.constant.AppConstants;
import com.codeinsight.common.result.ApiResponse;
import com.codeinsight.common.result.PageMeta;
import com.codeinsight.indexer.task.IndexingTaskProducer;
import com.codeinsight.model.dto.ProjectCreateRequest;
import com.codeinsight.model.dto.ProjectResponse;
import com.codeinsight.model.dto.TaskResponse;
import com.codeinsight.model.entity.AsyncTask;
import com.codeinsight.model.enums.TaskType;
import com.codeinsight.parser.git.ArchiveExtractService;
import com.codeinsight.security.jwt.UserPrincipal;
import com.codeinsight.service.AsyncTaskService;
import com.codeinsight.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final AsyncTaskService asyncTaskService;
    private final IndexingTaskProducer indexingTaskProducer;
    private final ArchiveExtractService archiveExtractService;

    @PostMapping
    public ApiResponse<ProjectResponse> createProject(
            @RequestBody @Valid ProjectCreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.ok(projectService.createProject(request, user.getId()));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> listProjects(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var clampedSize = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        Page<ProjectResponse> result = projectService.listProjects(user.getId(), PageRequest.of(page, clampedSize));
        return ApiResponse.ok(result.getContent(), new PageMeta(result.getTotalElements(), page, clampedSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> getProject(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.ok(projectService.getProject(id, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal user) {
        projectService.deleteProject(id, user.getId());
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/index")
    public ApiResponse<TaskResponse> triggerIndex(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal user) {
        projectService.getProject(id, user.getId()); // verify ownership
        projectService.updateIndexStatus(id, com.codeinsight.model.enums.IndexStatus.INDEXING);
        AsyncTask task = asyncTaskService.createTask(id, TaskType.INDEX_FULL);
        indexingTaskProducer.submitIndexTask(task.getId(), id, TaskType.INDEX_FULL);
        return ApiResponse.ok(asyncTaskService.getTask(task.getId()));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<ProjectResponse> uploadArchive(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal user) throws Exception {
        ProjectResponse project = projectService.getProject(id, user.getId());
        archiveExtractService.extract(file.getInputStream(), id, file.getOriginalFilename());
        return ApiResponse.ok(project);
    }
}
