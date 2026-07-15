package com.mt.friotrackapi.auth.controller;

import com.mt.friotrackapi.auth.dto.LoginRequest;
import com.mt.friotrackapi.auth.dto.LoginResponse;
import com.mt.friotrackapi.auth.service.AuthService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Sesion iniciada", authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(authService.currentUser());
    }
}
