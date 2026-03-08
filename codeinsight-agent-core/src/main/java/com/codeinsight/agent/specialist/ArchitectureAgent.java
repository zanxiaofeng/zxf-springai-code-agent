package com.codeinsight.agent.specialist;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.codeinsight.agent.tool.ASTAnalyzeTool;
import com.codeinsight.agent.tool.CodeSearchTool;
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
public class ArchitectureAgent {

    private final ChatClient chatClient;

    public ArchitectureAgent(
            ChatModel chatModel,
            ChatMemory chatMemory,
            @Qualifier("qwenMaxOptions") DashScopeChatOptions maxOptions,
            CodeSearchTool codeSearchTool,
            ASTAnalyzeTool astAnalyzeTool) {

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a software architecture expert. Your responsibilities:
                        1. Analyze module dependencies and coupling
                        2. Evaluate layered architecture compliance
                        3. Identify call chains and data flow
                        4. Assess scalability and maintainability
                        5. Generate architecture reports with diagrams (ASCII)

                        Use the provided tools to search and analyze code structure.
                        Answer in the same language as the user's question.
                        """)
                .defaultOptions(maxOptions)
                .defaultTools(codeSearchTool, astAnalyzeTool)
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
