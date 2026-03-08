package com.codeinsight.security;

import com.codeinsight.common.exception.BusinessException;
import com.codeinsight.model.dto.LoginRequest;
import com.codeinsight.model.dto.RegisterRequest;
import com.codeinsight.model.entity.User;
import com.codeinsight.model.enums.Role;
import com.codeinsight.model.repository.UserRepository;
import com.codeinsight.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUser() {
        var request = new RegisterRequest("testuser", "password123", "Test User", "test@example.com");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            var u = inv.getArgument(0, User.class);
            u.setId("u1");
            return u;
        });

        var user = authService.register(request);

        assertThat(user.getId()).isEqualTo("u1");
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getRole()).isEqualTo(Role.DEVELOPER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_shouldThrow() {
        var request = new RegisterRequest("existing", "pass", "name", "e@e.com");
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void login_shouldReturnToken() {
        var user = User.builder().id("u1").username("dev").passwordHash("hashed")
                .role(Role.DEVELOPER).enabled(true).build();
        when(userRepository.findByUsername("dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(tokenProvider.generateToken("u1", "dev", "DEVELOPER")).thenReturn("jwt-token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(userRepository.save(user)).thenReturn(user);

        var response = authService.login(new LoginRequest("dev", "pass"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("dev");
        assertThat(response.role()).isEqualTo("DEVELOPER");
    }

    @Test
    void login_invalidPassword_shouldThrow() {
        var user = User.builder().id("u1").username("dev").passwordHash("hashed")
                .role(Role.DEVELOPER).enabled(true).build();
        when(userRepository.findByUsername("dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("dev", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_disabledAccount_shouldThrow() {
        var user = User.builder().id("u1").username("dev").passwordHash("hashed")
                .role(Role.DEVELOPER).enabled(false).build();
        when(userRepository.findByUsername("dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("dev", "pass")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void login_userNotFound_shouldThrow() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", "pass")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void refreshToken_shouldReturnNewToken() {
        var user = User.builder().id("u1").username("dev").role(Role.DEVELOPER).enabled(true).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken("u1", "dev", "DEVELOPER")).thenReturn("new-token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        var response = authService.refreshToken("u1");

        assertThat(response.accessToken()).isEqualTo("new-token");
    }

    @Test
    void refreshToken_disabledUser_shouldThrow() {
        var user = User.builder().id("u1").username("dev").role(Role.DEVELOPER).enabled(false).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshToken("u1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disabled");
    }
}
