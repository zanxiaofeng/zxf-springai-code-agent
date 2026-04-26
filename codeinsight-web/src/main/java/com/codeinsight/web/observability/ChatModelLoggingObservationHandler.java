package com.codeinsight.web.observability;

import io.micrometer.observation.ObservationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Logs the full prompt and response for every ChatModel call at DEBUG level.
 */
@Component
@Slf4j
public class ChatModelLoggingObservationHandler implements ObservationHandler<ChatModelObservationContext>, Ordered {

    @Override
    public boolean supportsContext(io.micrometer.observation.Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

    @Override
    public void onStart(ChatModelObservationContext context) {
        Prompt prompt = context.getRequest();
        if (prompt != null && log.isDebugEnabled()) {
            var metadata = context.getOperationMetadata();
            log.debug("""
                    === LLM Request ===
                    Model: {}
                    Messages: {}
                    """,
                    metadata != null ? metadata.provider() : "unknown",
                    prompt.getInstructions().stream()
                            .map(m -> "[%s] %s".formatted(m.getMessageType(), truncate(m.getText(), 500)))
                            .toList());
        }
    }

    @Override
    public void onStop(ChatModelObservationContext context) {
        if (!log.isDebugEnabled()) return;

        ChatResponse response = context.getResponse();
        if (response == null) {
            log.debug("=== LLM Response: null ===");
            return;
        }

        var result = response.getResult();
        if (result != null && result.getOutput() != null) {
            log.debug("""
                    === LLM Response ===
                    Content: {}
                    FinishReason: {}
                    """,
                    truncate(result.getOutput().getText(), 1000),
                    result.getMetadata().getFinishReason());
        }

        var usage = response.getMetadata().getUsage();
        if (usage != null) {
            log.debug("=== Token Usage: prompt={}, completion={}, total={} ===",
                    usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        }
    }

    @Override
    public void onError(ChatModelObservationContext context) {
        log.error("=== LLM Error: {} ===", context.getError().getMessage());
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "<null>";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "... (truncated, total=" + text.length() + " chars)";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
