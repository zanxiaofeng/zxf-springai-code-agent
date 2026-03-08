package com.codeinsight.service;

import com.codeinsight.common.exception.ResourceNotFoundException;
import com.codeinsight.model.entity.Conversation;
import com.codeinsight.model.entity.ConversationMessage;
import com.codeinsight.model.entity.Project;
import com.codeinsight.model.entity.User;
import com.codeinsight.model.enums.MessageRole;
import com.codeinsight.model.enums.ScenarioType;
import com.codeinsight.model.repository.ConversationMessageRepository;
import com.codeinsight.model.repository.ConversationRepository;
import com.codeinsight.model.repository.ProjectRepository;
import com.codeinsight.model.repository.UserRepository;
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
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMessageRepository messageRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ConversationService conversationService;

    @Test
    void createConversation_shouldReturnConversation() {
        var project = Project.builder().id("p1").build();
        var user = User.builder().id("u1").username("dev").build();

        when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            var c = inv.getArgument(0, Conversation.class);
            c.setId("c1");
            return c;
        });

        var result = conversationService.createConversation("p1", "u1", ScenarioType.QA, "Test Title");

        assertThat(result.getId()).isEqualTo("c1");
        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.getScenarioType()).isEqualTo(ScenarioType.QA);
    }

    @Test
    void createConversation_projectNotFound_shouldThrow() {
        when(projectRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.createConversation("missing", "u1", ScenarioType.QA, "title"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addMessage_shouldSaveAndReturn() {
        var conversation = Conversation.builder().id("c1").build();
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(inv -> {
            var m = inv.getArgument(0, ConversationMessage.class);
            m.setId("m1");
            return m;
        });

        var message = conversationService.addMessage("c1", MessageRole.USER, "hello", null);

        assertThat(message.getId()).isEqualTo("m1");
        assertThat(message.getRole()).isEqualTo(MessageRole.USER);
        assertThat(message.getContent()).isEqualTo("hello");
    }

    @Test
    void addMessage_conversationNotFound_shouldThrow() {
        when(conversationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.addMessage("missing", MessageRole.USER, "hi", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMessages_shouldReturnList() {
        var msg = ConversationMessage.builder().id("m1").role(MessageRole.USER).content("test").build();
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc("c1")).thenReturn(List.of(msg));

        var messages = conversationService.getMessages("c1");

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().getContent()).isEqualTo("test");
    }

    @Test
    void listConversations_shouldReturnList() {
        var conv = Conversation.builder().id("c1").title("Test").build();
        when(conversationRepository.findByProjectIdAndUserIdOrderByUpdatedAtDesc("p1", "u1"))
                .thenReturn(List.of(conv));

        var result = conversationService.listConversations("p1", "u1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Test");
    }

    @Test
    void deleteConversation_shouldDeleteMessagesAndConversation() {
        when(conversationRepository.existsById("c1")).thenReturn(true);

        conversationService.deleteConversation("c1");

        verify(messageRepository).deleteByConversationId("c1");
        verify(conversationRepository).deleteById("c1");
    }

    @Test
    void deleteConversation_notFound_shouldThrow() {
        when(conversationRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> conversationService.deleteConversation("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
