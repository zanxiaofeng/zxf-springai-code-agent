package com.codeinsight.agent.specialist;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
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
public class QAAgent {

    private final ChatClient chatClient;

    public QAAgent(
            ChatModel chatModel,
            ChatMemory chatMemory,
            @Qualifier("qwenTurboOptions") DashScopeChatOptions turboOptions,
            CodeSearchTool codeSearchTool) {

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a senior Java code expert. Your role is to answer questions about the codebase.

                        Guidelines:
                        1. Use the searchCode tool to find relevant code before answering
                        2. Provide specific file paths and line numbers in your answers
                        3. Explain code logic clearly with examples
                        4. Point out potential issues or improvements when relevant
                        5. Answer in the same language as the user's question
                        """)
                .defaultOptions(turboOptions)
                .defaultTools(codeSearchTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public Flux<String> chat(String projectId, String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
