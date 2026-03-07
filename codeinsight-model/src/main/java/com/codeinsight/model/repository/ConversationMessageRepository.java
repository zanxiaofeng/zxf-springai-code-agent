package com.codeinsight.model.repository;

import com.codeinsight.model.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, String> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
