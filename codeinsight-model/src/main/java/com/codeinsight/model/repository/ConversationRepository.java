package com.codeinsight.model.repository;

import com.codeinsight.model.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    List<Conversation> findByProjectIdAndUserIdOrderByUpdatedAtDesc(String projectId, String userId);
}
