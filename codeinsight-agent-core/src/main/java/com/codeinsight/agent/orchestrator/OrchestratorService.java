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
    private final ChatPersistence chatPersistence;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public Flux<ChatEvent> chat(ChatRequest request, String userId) {
        var scenario = request.scenario() != null ? request.scenario() : ScenarioType.QA;
        var projectId = request.projectId();
        var message = request.message();

        var conversationId = chatPersistence.ensureConversation(
                request.conversationId(), projectId, userId, scenario, message);
        chatPersistence.saveUserMessage(conversationId, message);

        var agentName = resolveAgentName(scenario);
        var model = resolveModel(scenario);

        log.info("Chat request: scenario={}, agent={}, conversationId={}, projectId={}, userId={}",
                scenario, agentName, conversationId, projectId, userId);

        recordChatRequest(scenario);

        var metadata = Flux.just(ChatEvent.metadata(Map.of(
                "conversationId", conversationId,
                "agentName", agentName,
                "model", model,
                "scenario", scenario.name()
        )));

        var responseCollector = new StringBuilder();
        var content = routeToAgent(scenario, projectId, message, conversationId)
                .doOnNext(responseCollector::append)
                .map(ChatEvent::content)
                .doOnComplete(() -> chatPersistence.saveAssistantMessage(conversationId, responseCollector.toString()));

        var done = Flux.just(ChatEvent.done(Map.of("status", "completed")));

        return Flux.concat(metadata, content, done)
                .onErrorResume(e -> {
                    log.error("Chat error: {}", e.getMessage(), e);
                    recordChatError(scenario, e);
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

    private void recordChatRequest(ScenarioType scenario) {
        io.micrometer.core.instrument.Counter.builder("codeinsight.chat.requests")
                .tag("scenario", scenario.name())
                .register(meterRegistry).increment();
    }

    private void recordChatError(ScenarioType scenario, Throwable e) {
        var errorType = e.getClass().getSimpleName();
        io.micrometer.core.instrument.Counter.builder("codeinsight.chat.errors")
                .tag("scenario", scenario.name())
                .tag("error", errorType)
                .register(meterRegistry).increment();
    }
}
