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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public Conversation createConversation(String projectId, String userId, ScenarioType scenario, String title) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Conversation conversation = Conversation.builder()
                .title(title)
                .project(project)
                .user(user)
                .scenarioType(scenario)
                .build();

        return conversationRepository.save(conversation);
    }

    @Transactional
    public ConversationMessage addMessage(String conversationId, MessageRole role, String content, Map<String, Object> metadata) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        ConversationMessage message = ConversationMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .metadata(metadata)
                .build();

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> getMessages(String conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional(readOnly = true)
    public List<Conversation> listConversations(String projectId, String userId) {
        return conversationRepository.findByProjectIdAndUserIdOrderByUpdatedAtDesc(projectId, userId);
    }

    @Transactional
    public void deleteConversation(String conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new ResourceNotFoundException("Conversation", conversationId);
        }
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.deleteById(conversationId);
    }
}
