package com.codeinsight.service;

import com.codeinsight.indexer.task.TaskStatusUpdater;
import com.codeinsight.model.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskStatusUpdaterImpl implements TaskStatusUpdater {

    private final AsyncTaskService asyncTaskService;

    @Override
    public void onRunning(String taskId, int percent, String message) {
        log.debug("Task {} progress: {}% - {}", taskId, percent, message);
        asyncTaskService.updateStatus(taskId, TaskStatus.RUNNING, percent, message);
    }

    @Override
    public void onCompleted(String taskId) {
        log.info("Task {} completed", taskId);
        asyncTaskService.updateStatus(taskId, TaskStatus.COMPLETED, 100, "Indexing complete");
    }

    @Override
    public void onFailed(String taskId, String errorMessage) {
        log.error("Task {} failed: {}", taskId, errorMessage);
        asyncTaskService.updateStatus(taskId, TaskStatus.FAILED, -1, errorMessage);
    }
}
