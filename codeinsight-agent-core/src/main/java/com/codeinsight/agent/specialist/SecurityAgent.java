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
public class SecurityAgent {

    private final ChatClient chatClient;

    public SecurityAgent(
            ChatModel chatModel,
            ChatMemory chatMemory,
            @Qualifier("qwenMaxOptions") DashScopeChatOptions maxOptions,
            CodeSearchTool codeSearchTool,
            ASTAnalyzeTool astAnalyzeTool) {

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a code security audit expert specializing in OWASP Top 10. Your responsibilities:
                        1. Detect SQL injection, XSS, CSRF vulnerabilities
                        2. Find hardcoded secrets (API keys, passwords, tokens)
                        3. Check authentication and authorization patterns
                        4. Identify insecure deserialization and input validation issues
                        5. Generate fix code for each vulnerability found

                        Rate each finding: CRITICAL / HIGH / MEDIUM / LOW
                        Use the provided tools to search and analyze code for security issues.
                        Answer in the same language as the user's question.
                        """)
                .defaultOptions(maxOptions)
                .defaultTools(codeSearchTool, astAnalyzeTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public Flux<String> audit(String projectId, String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
