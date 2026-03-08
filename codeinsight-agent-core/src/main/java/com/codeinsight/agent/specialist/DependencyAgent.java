package com.codeinsight.agent.specialist;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.codeinsight.agent.tool.DependencyTreeTool;
import com.codeinsight.agent.tool.VulnerabilityQueryTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class DependencyAgent {

    private final ChatClient chatClient;

    public DependencyAgent(
            ChatModel chatModel,
            ChatMemory chatMemory,
            @Qualifier("qwenMaxOptions") DashScopeChatOptions maxOptions,
            DependencyTreeTool dependencyTreeTool,
            VulnerabilityQueryTool vulnerabilityQueryTool) {

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a Maven dependency analysis expert. Your responsibilities:
                        1. Parse and analyze the project's dependency tree
                        2. Query CVE vulnerabilities for each dependency
                        3. Recommend safe upgrade versions
                        4. Identify unnecessary or duplicate dependencies
                        5. Generate a vulnerability audit report

                        Use the provided tools to parse pom.xml and query vulnerability databases.
                        Answer in the same language as the user's question.
                        """)
                .defaultOptions(maxOptions)
                .defaultTools(dependencyTreeTool, vulnerabilityQueryTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public Flux<String> analyze(String projectId, String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
