package com.codeinsight.web.controller;

import com.codeinsight.common.result.ApiResponse;
import com.codeinsight.model.dto.ConversationResponse;
import com.codeinsight.model.dto.MessageResponse;
import com.codeinsight.model.entity.Conversation;
import com.codeinsight.model.entity.ConversationMessage;
import com.codeinsight.security.jwt.UserPrincipal;
import com.codeinsight.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ApiResponse<List<ConversationResponse>> listConversations(
            @RequestParam String projectId,
            @AuthenticationPrincipal UserPrincipal user) {
        var conversations = conversationService.listConversations(projectId, user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(conversations);
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(@PathVariable String id) {
        var messages = conversationService.getMessages(id)
                .stream()
                .map(this::toMessageResponse)
                .toList();
        return ApiResponse.ok(messages);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConversation(@PathVariable String id) {
        conversationService.deleteConversation(id);
        return ApiResponse.ok(null);
    }

    private ConversationResponse toResponse(Conversation c) {
        return new ConversationResponse(
                c.getId(), c.getTitle(), c.getProject().getId(),
                c.getScenarioType(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private MessageResponse toMessageResponse(ConversationMessage m) {
        return new MessageResponse(
                m.getId(), m.getRole(), m.getContent(),
                m.getTokenCount(), m.getCreatedAt());
    }
}
