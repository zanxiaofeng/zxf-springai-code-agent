package com.codeinsight.model.repository;

import com.codeinsight.model.entity.AsyncTask;
import com.codeinsight.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsyncTaskRepository extends JpaRepository<AsyncTask, String> {

    List<AsyncTask> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<AsyncTask> findByProjectIdAndStatus(String projectId, TaskStatus status);
}
