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
public class CodeGenAgent {

    private final ChatClient chatClient;

    public CodeGenAgent(
            ChatModel chatModel,
            ChatMemory chatMemory,
            @Qualifier("qwenMaxOptions") DashScopeChatOptions maxOptions,
            CodeSearchTool codeSearchTool,
            ASTAnalyzeTool astAnalyzeTool) {

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a Java code generation and refactoring expert. Your responsibilities:
                        1. Generate code following existing project style and conventions
                        2. Suggest refactoring strategies with before/after examples
                        3. Apply design patterns appropriately
                        4. Ensure generated code follows SOLID principles
                        5. Include proper error handling and input validation

                        Use the provided tools to understand existing code patterns before generating new code.
                        Answer in the same language as the user's question.
                        """)
                .defaultOptions(maxOptions)
                .defaultTools(codeSearchTool, astAnalyzeTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public Flux<String> generate(String projectId, String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
