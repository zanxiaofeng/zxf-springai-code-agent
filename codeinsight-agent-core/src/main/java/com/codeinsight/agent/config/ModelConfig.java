package com.codeinsight.agent.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ModelConfig {

    @Bean("qwenMaxModel")
    @Primary
    public ChatModel qwenMaxModel(DashScopeChatModel autoConfiguredModel) {
        // The auto-configured DashScopeChatModel uses qwen-max by default via application.yml
        // For explicit model selection, we create different ChatClient instances with different options
        return autoConfiguredModel;
    }

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
}
