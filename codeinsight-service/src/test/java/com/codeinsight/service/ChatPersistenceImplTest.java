package com.codeinsight.service;

import com.codeinsight.model.entity.Conversation;
import com.codeinsight.model.enums.MessageRole;
import com.codeinsight.model.enums.ScenarioType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatPersistenceImplTest {

    @Mock
    private ConversationService conversationService;
    @InjectMocks
    private ChatPersistenceImpl chatPersistence;

    @Test
    void ensureConversation_withExistingId_shouldReturnSameId() {
        var result = chatPersistence.ensureConversation("c1", "p1", "u1", ScenarioType.QA, "hello");

        assertThat(result).isEqualTo("c1");
        verifyNoInteractions(conversationService);
    }

    @Test
    void ensureConversation_withNullId_shouldCreateNew() {
        var conversation = Conversation.builder().id("new-c1").build();
        when(conversationService.createConversation("p1", "u1", ScenarioType.QA, "hello"))
                .thenReturn(conversation);

        var result = chatPersistence.ensureConversation(null, "p1", "u1", ScenarioType.QA, "hello");

        assertThat(result).isEqualTo("new-c1");
    }

    @Test
    void ensureConversation_withBlankId_shouldCreateNew() {
        var conversation = Conversation.builder().id("new-c2").build();
        when(conversationService.createConversation("p1", "u1", ScenarioType.REVIEW, "review this"))
                .thenReturn(conversation);

        var result = chatPersistence.ensureConversation("  ", "p1", "u1", ScenarioType.REVIEW, "review this");

        assertThat(result).isEqualTo("new-c2");
    }

    @Test
    void ensureConversation_longMessage_shouldTruncateTitle() {
        var longMessage = "a".repeat(200);
        var conversation = Conversation.builder().id("c3").build();
        when(conversationService.createConversation(eq("p1"), eq("u1"), eq(ScenarioType.QA), argThat(t -> t.length() <= 100)))
                .thenReturn(conversation);

        chatPersistence.ensureConversation(null, "p1", "u1", ScenarioType.QA, longMessage);

        verify(conversationService).createConversation(eq("p1"), eq("u1"), eq(ScenarioType.QA), argThat(t -> t.length() <= 100));
    }

    @Test
    void saveUserMessage_shouldDelegateToService() {
        chatPersistence.saveUserMessage("c1", "hello");

        verify(conversationService).addMessage("c1", MessageRole.USER, "hello", null);
    }

    @Test
    void saveAssistantMessage_shouldDelegateToService() {
        chatPersistence.saveAssistantMessage("c1", "response");

        verify(conversationService).addMessage("c1", MessageRole.ASSISTANT, "response", null);
    }
}
