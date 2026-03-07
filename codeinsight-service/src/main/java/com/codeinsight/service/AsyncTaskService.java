package com.codeinsight.service;

import com.codeinsight.common.exception.ResourceNotFoundException;
import com.codeinsight.model.dto.TaskResponse;
import com.codeinsight.model.entity.AsyncTask;
import com.codeinsight.model.enums.TaskStatus;
import com.codeinsight.model.enums.TaskType;
import com.codeinsight.model.repository.AsyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncTaskService {

    private final AsyncTaskRepository taskRepository;

    @Transactional
    public AsyncTask createTask(String projectId, TaskType taskType) {
        AsyncTask task = AsyncTask.builder()
                .projectId(projectId)
                .taskType(taskType)
                .status(TaskStatus.PENDING)
                .progressPercent(0)
                .build();
        return taskRepository.save(task);
    }

    @Transactional
    public void updateStatus(String taskId, TaskStatus status, int percent, String message) {
        AsyncTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("AsyncTask", taskId));

        task.setStatus(status);
        task.setProgressPercent(percent);
        task.setProgressMessage(message);

        if (status == TaskStatus.RUNNING && task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
            task.setCompletedAt(LocalDateTime.now());
        }
        if (status == TaskStatus.FAILED) {
            task.setErrorMessage(message);
        }

        taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(String taskId) {
        AsyncTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("AsyncTask", taskId));
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasksByProject(String projectId) {
        return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TaskResponse toResponse(AsyncTask task) {
        return new TaskResponse(
                task.getId(),
                task.getTaskType(),
                task.getProjectId(),
                task.getStatus(),
                task.getProgressPercent(),
                task.getProgressMessage(),
                task.getErrorMessage(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getCreatedAt()
        );
    }
}
