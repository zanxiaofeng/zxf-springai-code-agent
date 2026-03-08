package com.codeinsight.web.controller;

import com.codeinsight.common.result.ApiResponse;
import com.codeinsight.model.dto.LoginRequest;
import com.codeinsight.model.dto.LoginResponse;
import com.codeinsight.model.dto.RegisterRequest;
import com.codeinsight.model.entity.User;
import com.codeinsight.security.AuthService;
import com.codeinsight.security.jwt.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody @Valid RegisterRequest request) {
        User user = authService.register(request);
        return ApiResponse.ok(user.getId());
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.ok(authService.refreshToken(user.getId()));
    }
}
