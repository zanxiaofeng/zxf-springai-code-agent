package com.codeinsight.indexer.task;

import com.codeinsight.common.constant.AppConstants;
import com.codeinsight.indexer.pipeline.IndexingPipeline;
import com.codeinsight.model.enums.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndexingTaskConsumer implements InitializingBean {

    private final StringRedisTemplate redisTemplate;
    private final IndexingPipeline indexingPipeline;
    private final TaskStatusUpdater taskStatusUpdater;

    private static final String CONSUMER_NAME = "consumer-1";

    @Override
    public void afterPropertiesSet() {
        try {
            redisTemplate.opsForStream().createGroup(AppConstants.REDIS_STREAM_INDEXING, AppConstants.REDIS_CONSUMER_GROUP);
        } catch (Exception e) {
            log.debug("Consumer group already exists or stream not yet created");
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void consumeTasks() {
        try {
            @SuppressWarnings("unchecked")
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(Consumer.from(AppConstants.REDIS_CONSUMER_GROUP, CONSUMER_NAME),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(AppConstants.REDIS_STREAM_INDEXING, ReadOffset.lastConsumed()));

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                try {
                    Map<String, String> taskData = new java.util.HashMap<>();
                    record.getValue().forEach((k, v) -> taskData.put(String.valueOf(k), String.valueOf(v)));
                    processTask(taskData);
                    redisTemplate.opsForStream().acknowledge(
                            AppConstants.REDIS_STREAM_INDEXING, AppConstants.REDIS_CONSUMER_GROUP, record.getId());
                } catch (Exception e) {
                    log.error("Failed to process indexing task: {}", record.getValue(), e);
                }
            }
        } catch (Exception e) {
            log.debug("No messages available or stream does not exist yet");
        }
    }

    private void processTask(Map<String, String> taskData) {
        String taskId = taskData.get("taskId");
        String projectId = taskData.get("projectId");
        TaskType taskType = TaskType.valueOf(taskData.get("taskType"));

        log.info("Processing indexing task: taskId={}, projectId={}, taskType={}", taskId, projectId, taskType);
        taskStatusUpdater.onRunning(taskId, 0, "Starting indexing...");

        try {
            indexingPipeline.execute(projectId, taskType, (percent, msg) ->
                    taskStatusUpdater.onRunning(taskId, percent, msg));
            taskStatusUpdater.onCompleted(taskId);
            log.info("Indexing task completed: taskId={}, projectId={}", taskId, projectId);
        } catch (Exception e) {
            log.error("Indexing failed for project {} (taskId={}): {}", projectId, taskId, e.getMessage(), e);
            taskStatusUpdater.onFailed(taskId, e.getMessage());
        }
    }
}
