package com.codeinsight.security;

import com.codeinsight.common.exception.BusinessException;
import com.codeinsight.model.dto.LoginRequest;
import com.codeinsight.model.dto.LoginResponse;
import com.codeinsight.model.dto.RegisterRequest;
import com.codeinsight.model.entity.User;
import com.codeinsight.model.enums.Role;
import com.codeinsight.model.repository.UserRepository;
import com.codeinsight.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already exists: " + request.username());
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .email(request.email())
                .role(Role.DEVELOPER)
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }

        if (!user.getEnabled()) {
            throw new BusinessException("Account is disabled");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildLoginResponse(user);
    }

    public LoginResponse refreshToken(String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.getEnabled()) {
            throw new BusinessException("Account is disabled");
        }

        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        var token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new LoginResponse(token, tokenProvider.getExpirationMs() / 1000, user.getUsername(), user.getRole().name());
    }
}
