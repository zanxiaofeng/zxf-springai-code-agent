package com.codeinsight.agent.orchestrator;

import com.codeinsight.model.enums.ScenarioType;

/**
 * Interface for persisting chat conversations, implemented by the service layer.
 */
public interface ChatPersistence {

    /**
     * Ensure a conversation exists. If conversationId is null, create a new one.
     * @return the conversation ID (existing or newly created)
     */
    String ensureConversation(String conversationId, String projectId, String userId, ScenarioType scenario, String firstMessage);

    void saveUserMessage(String conversationId, String content);

    void saveAssistantMessage(String conversationId, String content);
}
