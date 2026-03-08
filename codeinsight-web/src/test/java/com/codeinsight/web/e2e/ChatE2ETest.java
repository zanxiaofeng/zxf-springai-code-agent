package com.codeinsight.web.e2e;

import com.codeinsight.model.dto.ChatRequest;
import com.codeinsight.model.dto.ProjectCreateRequest;
import com.codeinsight.model.enums.ScenarioType;
import com.codeinsight.model.enums.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatE2ETest extends BaseE2ETest {

    private String authToken;
    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        authToken = registerAndLogin("chatuser_" + System.nanoTime(), "password123");

        ProjectCreateRequest projectRequest = new ProjectCreateRequest(
                "Chat Test Project", "For chat testing", SourceType.GIT,
                "https://github.com/example/chat-test.git", "main");

        String response = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        projectId = objectMapper.readTree(response).get("data").get("id").asText();
    }

    @Test
    void shouldReturnSSEStreamForChatRequest() throws Exception {
        ChatRequest chatRequest = new ChatRequest(projectId, null, "What does OrderService do?", ScenarioType.QA);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/chat/stream")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void shouldRejectUnauthenticatedChat() throws Exception {
        ChatRequest chatRequest = new ChatRequest(projectId, null, "test", ScenarioType.QA);

        mockMvc.perform(post("/api/v1/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectChatWithoutMessage() throws Exception {
        ChatRequest chatRequest = new ChatRequest(projectId, null, null, ScenarioType.QA);

        mockMvc.perform(post("/api/v1/chat/stream")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isBadRequest());
    }
}
