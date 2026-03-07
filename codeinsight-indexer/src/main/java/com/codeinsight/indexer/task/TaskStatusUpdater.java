package com.codeinsight.indexer.task;

public interface TaskStatusUpdater {

    void onRunning(String taskId, int percent, String message);

    void onCompleted(String taskId);

    void onFailed(String taskId, String errorMessage);
}
