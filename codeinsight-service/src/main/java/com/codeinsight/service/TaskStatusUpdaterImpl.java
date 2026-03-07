package com.codeinsight.service;

import com.codeinsight.indexer.task.TaskStatusUpdater;
import com.codeinsight.model.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskStatusUpdaterImpl implements TaskStatusUpdater {

    private final AsyncTaskService asyncTaskService;

    @Override
    public void onRunning(String taskId, int percent, String message) {
        asyncTaskService.updateStatus(taskId, TaskStatus.RUNNING, percent, message);
    }

    @Override
    public void onCompleted(String taskId) {
        asyncTaskService.updateStatus(taskId, TaskStatus.COMPLETED, 100, "Indexing complete");
    }

    @Override
    public void onFailed(String taskId, String errorMessage) {
        asyncTaskService.updateStatus(taskId, TaskStatus.FAILED, -1, errorMessage);
    }
}
