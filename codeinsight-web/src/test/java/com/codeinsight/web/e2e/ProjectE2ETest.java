package com.codeinsight.web.e2e;

import com.codeinsight.model.dto.ProjectCreateRequest;
import com.codeinsight.model.enums.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectE2ETest extends BaseE2ETest {

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        authToken = registerAndLogin("projectuser_" + System.nanoTime(), "password123");
    }

    @Test
    void shouldCreateProject() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "Test Project", "A test project", SourceType.GIT,
                "https://github.com/example/test.git", "main");

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Project"))
                .andExpect(jsonPath("$.data.sourceType").value("GIT"))
                .andExpect(jsonPath("$.data.indexStatus").value("PENDING"));
    }

    @Test
    void shouldListProjects() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "List Project", "For listing", SourceType.GIT,
                "https://github.com/example/list.git", "main");

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldGetProjectById() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "Get Project", "For getting", SourceType.GIT,
                "https://github.com/example/get.git", "main");

        String response = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(response).get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(projectId))
                .andExpect(jsonPath("$.data.name").value("Get Project"));
    }

    @Test
    void shouldDeleteProject() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "Delete Project", "For deleting", SourceType.GIT,
                "https://github.com/example/del.git", "main");

        String response = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(response).get("data").get("id").asText();

        mockMvc.perform(delete("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}
