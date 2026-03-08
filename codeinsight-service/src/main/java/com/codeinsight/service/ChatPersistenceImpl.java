package com.codeinsight.service;

import com.codeinsight.agent.orchestrator.ChatPersistence;
import com.codeinsight.model.entity.Conversation;
import com.codeinsight.model.enums.MessageRole;
import com.codeinsight.model.enums.ScenarioType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatPersistenceImpl implements ChatPersistence {

    private final ConversationService conversationService;

    @Override
    public String ensureConversation(String conversationId, String projectId, String userId, ScenarioType scenario, String firstMessage) {
        if (StringUtils.isNotBlank(conversationId)) {
            return conversationId;
        }
        String title = StringUtils.abbreviate(firstMessage, 100);
        Conversation conversation = conversationService.createConversation(projectId, userId, scenario, title);
        log.info("Created new conversation: id={}, projectId={}", conversation.getId(), projectId);
        return conversation.getId();
    }

    @Override
    public void saveUserMessage(String conversationId, String content) {
        conversationService.addMessage(conversationId, MessageRole.USER, content, null);
    }

    @Override
    public void saveAssistantMessage(String conversationId, String content) {
        conversationService.addMessage(conversationId, MessageRole.ASSISTANT, content, null);
    }
}
