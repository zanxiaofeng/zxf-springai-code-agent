package com.codeinsight.indexer.task;

import com.codeinsight.common.constant.AppConstants;
import com.codeinsight.model.enums.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndexingTaskProducer {

    private final StringRedisTemplate redisTemplate;

    public void submitIndexTask(String taskId, String projectId, TaskType taskType) {
        Map<String, String> message = Map.of(
                "taskId", taskId,
                "projectId", projectId,
                "taskType", taskType.name(),
                "submittedAt", Instant.now().toString()
        );

        redisTemplate.opsForStream().add(
                StreamRecords.string(message).withStreamKey(AppConstants.REDIS_STREAM_INDEXING));

        log.info("Submitted indexing task: taskId={}, projectId={}", taskId, projectId);
    }
}
