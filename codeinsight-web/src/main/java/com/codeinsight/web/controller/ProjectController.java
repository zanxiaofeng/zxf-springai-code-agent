package com.codeinsight.web.controller;

import com.codeinsight.common.result.ApiResponse;
import com.codeinsight.common.result.PageMeta;
import com.codeinsight.indexer.task.IndexingTaskProducer;
import com.codeinsight.model.dto.ProjectCreateRequest;
import com.codeinsight.model.dto.ProjectResponse;
import com.codeinsight.model.dto.TaskResponse;
import com.codeinsight.model.entity.AsyncTask;
import com.codeinsight.model.enums.TaskType;
import com.codeinsight.security.jwt.UserPrincipal;
import com.codeinsight.service.AsyncTaskService;
import com.codeinsight.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final AsyncTaskService asyncTaskService;
    private final IndexingTaskProducer indexingTaskProducer;

    @PostMapping
    public ApiResponse<ProjectResponse> createProject(
            @RequestBody @Valid ProjectCreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ProjectResponse response = projectService.createProject(request, user.getId());
        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> listProjects(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProjectResponse> result = projectService.listProjects(user.getId(), PageRequest.of(page, size));
        PageMeta meta = new PageMeta(result.getTotalElements(), page, size);
        return ApiResponse.ok(result.getContent(), meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> getProject(@PathVariable String id) {
        return ApiResponse.ok(projectService.getProject(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/index")
    public ApiResponse<TaskResponse> triggerIndex(@PathVariable String id) {
        projectService.updateIndexStatus(id, com.codeinsight.model.enums.IndexStatus.INDEXING);
        AsyncTask task = asyncTaskService.createTask(id, TaskType.INDEX_FULL);
        indexingTaskProducer.submitIndexTask(task.getId(), id, TaskType.INDEX_FULL);
        return ApiResponse.ok(asyncTaskService.getTask(task.getId()));
    }
}
