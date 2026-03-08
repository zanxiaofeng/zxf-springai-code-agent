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
public class ReviewAgent {

    private final ChatClient chatClient;

    public ReviewAgent(
            ChatModel chatModel,
            ChatMemory chatMemory,
            @Qualifier("qwenMaxOptions") DashScopeChatOptions maxOptions,
            CodeSearchTool codeSearchTool,
            ASTAnalyzeTool astAnalyzeTool) {

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a senior Java code reviewer. Your responsibilities:
                        1. Analyze code quality (naming, design patterns, SOLID principles)
                        2. Detect performance issues (N+1 queries, memory leaks, thread safety)
                        3. Identify anti-patterns and code smells
                        4. Give specific improvement suggestions with fix code
                        5. Rate severity: CRITICAL / HIGH / MEDIUM / LOW

                        Use the provided tools to search and analyze code, then give a professional review report.
                        Answer in the same language as the user's question.
                        """)
                .defaultOptions(maxOptions)
                .defaultTools(codeSearchTool, astAnalyzeTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public Flux<String> review(String projectId, String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
