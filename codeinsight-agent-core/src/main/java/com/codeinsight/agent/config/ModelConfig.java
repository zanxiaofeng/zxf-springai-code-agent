package com.codeinsight.agent.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfig {

    @Bean
    public DashScopeChatOptions qwenMaxOptions() {
        return DashScopeChatOptions.builder()
                .model("qwen-max")
                .temperature(0.3)
                .build();
    }

    @Bean
    public DashScopeChatOptions qwenTurboOptions() {
        return DashScopeChatOptions.builder()
                .model("qwen-turbo")
                .temperature(0.7)
                .build();
    }

    @Bean("qwenMaxClient")
    public ChatClient qwenMaxClient(ChatModel chatModel, DashScopeChatOptions qwenMaxOptions) {
        return ChatClient.builder(chatModel)
                .defaultOptions(qwenMaxOptions)
                .build();
    }

    @Bean("qwenTurboClient")
    public ChatClient qwenTurboClient(ChatModel chatModel, DashScopeChatOptions qwenTurboOptions) {
        return ChatClient.builder(chatModel)
                .defaultOptions(qwenTurboOptions)
                .build();
    }
}
