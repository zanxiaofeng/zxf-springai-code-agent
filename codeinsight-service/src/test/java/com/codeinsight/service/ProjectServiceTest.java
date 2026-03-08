package com.codeinsight.service;

import com.codeinsight.common.exception.ResourceNotFoundException;
import com.codeinsight.model.dto.ProjectCreateRequest;
import com.codeinsight.model.dto.ProjectResponse;
import com.codeinsight.model.entity.Project;
import com.codeinsight.model.entity.User;
import com.codeinsight.model.enums.IndexStatus;
import com.codeinsight.model.enums.SourceType;
import com.codeinsight.model.repository.ProjectRepository;
import com.codeinsight.model.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_shouldReturnResponse() {
        var user = User.builder().id("u1").username("dev").build();
        var request = new ProjectCreateRequest("my-project", "desc", SourceType.GIT, "https://github.com/test/repo.git", "main");

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            var p = inv.getArgument(0, Project.class);
            p.setId("p1");
            return p;
        });

        var response = projectService.createProject(request, "u1");

        assertThat(response.id()).isEqualTo("p1");
        assertThat(response.name()).isEqualTo("my-project");
        assertThat(response.sourceType()).isEqualTo(SourceType.GIT);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProject_userNotFound_shouldThrow() {
        var request = new ProjectCreateRequest("p", "d", SourceType.GIT, "url", "main");
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(request, "missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProject_shouldReturnResponse() {
        var project = Project.builder().id("p1").name("test").sourceType(SourceType.GIT)
                .indexStatus(IndexStatus.PENDING).build();
        when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

        var response = projectService.getProject("p1");

        assertThat(response.id()).isEqualTo("p1");
        assertThat(response.name()).isEqualTo("test");
    }

    @Test
    void getProject_notFound_shouldThrow() {
        when(projectRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listProjects_shouldReturnPage() {
        var project = Project.builder().id("p1").name("test").sourceType(SourceType.GIT)
                .indexStatus(IndexStatus.PENDING).build();
        var pageable = PageRequest.of(0, 10);
        when(projectRepository.findByOwnerId("u1", pageable))
                .thenReturn(new PageImpl<>(List.of(project)));

        Page<ProjectResponse> result = projectService.listProjects("u1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo("p1");
    }

    @Test
    void deleteProject_shouldDelete() {
        when(projectRepository.existsById("p1")).thenReturn(true);

        projectService.deleteProject("p1");

        verify(projectRepository).deleteById("p1");
    }

    @Test
    void deleteProject_notFound_shouldThrow() {
        when(projectRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> projectService.deleteProject("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateIndexStatus_shouldUpdateAndSave() {
        var project = Project.builder().id("p1").indexStatus(IndexStatus.PENDING).build();
        when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        projectService.updateIndexStatus("p1", IndexStatus.INDEXING);

        assertThat(project.getIndexStatus()).isEqualTo(IndexStatus.INDEXING);
        verify(projectRepository).save(project);
    }
}
