package com.codeinsight.service;

import com.codeinsight.common.exception.BusinessException;
import com.codeinsight.common.exception.ResourceNotFoundException;
import com.codeinsight.model.entity.AsyncTask;
import com.codeinsight.model.enums.TaskStatus;
import com.codeinsight.model.enums.TaskType;
import com.codeinsight.model.repository.AsyncTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncTaskServiceTest {

    @Mock
    private AsyncTaskRepository taskRepository;
    @InjectMocks
    private AsyncTaskService asyncTaskService;

    @Test
    void createTask_shouldSaveAndReturn() {
        when(taskRepository.save(any(AsyncTask.class))).thenAnswer(inv -> {
            var task = inv.getArgument(0, AsyncTask.class);
            task.setId("t1");
            return task;
        });

        var task = asyncTaskService.createTask("p1", TaskType.INDEX_FULL);

        assertThat(task.getId()).isEqualTo("t1");
        assertThat(task.getProjectId()).isEqualTo("p1");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        verify(taskRepository).save(any(AsyncTask.class));
    }

    @Test
    void updateStatus_toRunning_shouldSetStartedAt() {
        var task = AsyncTask.builder().id("t1").status(TaskStatus.PENDING).build();
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));

        asyncTaskService.updateStatus("t1", TaskStatus.RUNNING, 10, "Processing...");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(task.getStartedAt()).isNotNull();
        assertThat(task.getProgressPercent()).isEqualTo(10);
        verify(taskRepository).save(task);
    }

    @Test
    void updateStatus_toCompleted_shouldSetCompletedAt() {
        var task = AsyncTask.builder().id("t1").status(TaskStatus.RUNNING).build();
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));

        asyncTaskService.updateStatus("t1", TaskStatus.COMPLETED, 100, "Done");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    void updateStatus_toFailed_shouldSetErrorMessage() {
        var task = AsyncTask.builder().id("t1").status(TaskStatus.RUNNING).build();
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));

        asyncTaskService.updateStatus("t1", TaskStatus.FAILED, 50, "Error occurred");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo("Error occurred");
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    void updateStatus_notFound_shouldThrow() {
        when(taskRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asyncTaskService.updateStatus("missing", TaskStatus.RUNNING, 0, ""))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTask_shouldReturnResponse() {
        var task = AsyncTask.builder().id("t1").projectId("p1").taskType(TaskType.INDEX_FULL)
                .status(TaskStatus.PENDING).progressPercent(0).build();
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));

        var response = asyncTaskService.getTask("t1");

        assertThat(response.id()).isEqualTo("t1");
        assertThat(response.taskType()).isEqualTo(TaskType.INDEX_FULL);
        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void listTasksByProject_shouldReturnList() {
        var task = AsyncTask.builder().id("t1").projectId("p1").taskType(TaskType.INDEX_FULL)
                .status(TaskStatus.COMPLETED).progressPercent(100).build();
        when(taskRepository.findByProjectIdOrderByCreatedAtDesc("p1")).thenReturn(List.of(task));

        var result = asyncTaskService.listTasksByProject("p1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("t1");
    }

    @Test
    void cancelTask_pending_shouldCancel() {
        var task = AsyncTask.builder().id("t1").status(TaskStatus.PENDING).build();
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        var response = asyncTaskService.cancelTask("t1");

        assertThat(response.status()).isEqualTo(TaskStatus.CANCELLED);
    }

    @Test
    void cancelTask_running_shouldCancel() {
        var task = AsyncTask.builder().id("t1").status(TaskStatus.RUNNING).build();
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        var response = asyncTaskService.cancelTask("t1");

        assertThat(response.status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    void cancelTask_completed_shouldThrow() {
        var task = AsyncTask.builder().id("t1").status(TaskStatus.COMPLETED).build();
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> asyncTaskService.cancelTask("t1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void cancelTask_notFound_shouldThrow() {
        when(taskRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asyncTaskService.cancelTask("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
