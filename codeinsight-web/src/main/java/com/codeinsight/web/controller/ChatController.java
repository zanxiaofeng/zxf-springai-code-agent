package com.codeinsight.web.controller;

import com.codeinsight.agent.orchestrator.OrchestratorService;
import com.codeinsight.model.dto.ChatRequest;
import com.codeinsight.security.jwt.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final OrchestratorService orchestratorService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestBody @Valid ChatRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        return orchestratorService.chat(request, user.getId())
                .map(event -> ServerSentEvent.<String>builder()
                        .event(event.type())
                        .data(event.toJson())
                        .build());
    }
}
