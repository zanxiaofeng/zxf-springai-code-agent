package com.codeinsight.agent.orchestrator;

import com.codeinsight.agent.specialist.QAAgent;
import com.codeinsight.model.dto.ChatEvent;
import com.codeinsight.model.dto.ChatRequest;
import com.codeinsight.model.enums.ScenarioType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorService {

    private final QAAgent qaAgent;

    public Flux<ChatEvent> chat(ChatRequest request, String userId) {
        ScenarioType scenario = request.scenario() != null ? request.scenario() : ScenarioType.QA;
        String conversationId = request.conversationId() != null ? request.conversationId() : java.util.UUID.randomUUID().toString();

        log.info("Chat request: scenario={}, projectId={}, userId={}", scenario, request.projectId(), userId);

        Flux<ChatEvent> metadata = Flux.just(ChatEvent.metadata(Map.of(
                "conversationId", conversationId,
                "agentName", "qa-agent",
                "model", "qwen-turbo",
                "scenario", scenario.name()
        )));

        Flux<ChatEvent> content = switch (scenario) {
            case QA -> qaAgent.chat(request.projectId(), request.message(), conversationId)
                    .map(ChatEvent::content);
            default -> Flux.just(ChatEvent.content("Scenario " + scenario + " is not yet implemented. Only QA is available in P0."));
        };

        Flux<ChatEvent> done = Flux.just(ChatEvent.done(Map.of("status", "completed")));

        return Flux.concat(metadata, content, done)
                .onErrorResume(e -> {
                    log.error("Chat error: {}", e.getMessage(), e);
                    return Flux.just(ChatEvent.error(e.getMessage()));
                });
    }
}
