package com.codeinsight.agent.orchestrator;

import com.codeinsight.agent.specialist.*;
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
    private final ReviewAgent reviewAgent;
    private final ArchitectureAgent architectureAgent;
    private final CodeGenAgent codeGenAgent;
    private final DependencyAgent dependencyAgent;
    private final SecurityAgent securityAgent;

    public Flux<ChatEvent> chat(ChatRequest request, String userId) {
        ScenarioType scenario = request.scenario() != null ? request.scenario() : ScenarioType.QA;
        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : java.util.UUID.randomUUID().toString();
        String projectId = request.projectId();
        String message = request.message();

        String agentName = resolveAgentName(scenario);
        String model = resolveModel(scenario);

        log.info("Chat request: scenario={}, agent={}, projectId={}, userId={}", scenario, agentName, projectId, userId);

        Flux<ChatEvent> metadata = Flux.just(ChatEvent.metadata(Map.of(
                "conversationId", conversationId,
                "agentName", agentName,
                "model", model,
                "scenario", scenario.name()
        )));

        Flux<ChatEvent> content = routeToAgent(scenario, projectId, message, conversationId)
                .map(ChatEvent::content);

        Flux<ChatEvent> done = Flux.just(ChatEvent.done(Map.of("status", "completed")));

        return Flux.concat(metadata, content, done)
                .onErrorResume(e -> {
                    log.error("Chat error: {}", e.getMessage(), e);
                    return Flux.just(ChatEvent.error(e.getMessage()));
                });
    }

    private Flux<String> routeToAgent(ScenarioType scenario, String projectId, String message, String conversationId) {
        return switch (scenario) {
            case QA -> qaAgent.chat(projectId, message, conversationId);
            case REVIEW -> reviewAgent.review(projectId, message, conversationId);
            case ARCHITECTURE -> architectureAgent.analyze(projectId, message, conversationId);
            case CODEGEN -> codeGenAgent.generate(projectId, message, conversationId);
            case DEPENDENCY -> dependencyAgent.analyze(projectId, message, conversationId);
            case SECURITY -> securityAgent.audit(projectId, message, conversationId);
        };
    }

    private String resolveAgentName(ScenarioType scenario) {
        return switch (scenario) {
            case QA -> "qa-agent";
            case REVIEW -> "review-agent";
            case ARCHITECTURE -> "architecture-agent";
            case CODEGEN -> "codegen-agent";
            case DEPENDENCY -> "dependency-agent";
            case SECURITY -> "security-agent";
        };
    }

    private String resolveModel(ScenarioType scenario) {
        return switch (scenario) {
            case QA -> "qwen-turbo";
            case REVIEW, ARCHITECTURE, CODEGEN, DEPENDENCY, SECURITY -> "qwen-max";
        };
    }
}
