package com.codeinsight.web.controller;

import com.codeinsight.common.result.ApiResponse;
import com.codeinsight.model.dto.TaskResponse;
import com.codeinsight.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final AsyncTaskService asyncTaskService;

    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> getTask(@PathVariable String id) {
        return ApiResponse.ok(asyncTaskService.getTask(id));
    }

    @GetMapping
    public ApiResponse<List<TaskResponse>> listTasks(@RequestParam String projectId) {
        return ApiResponse.ok(asyncTaskService.listTasksByProject(projectId));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<TaskResponse> cancelTask(@PathVariable String id) {
        return ApiResponse.ok(asyncTaskService.cancelTask(id));
    }
}
