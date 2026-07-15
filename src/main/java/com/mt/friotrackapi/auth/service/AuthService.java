package com.mt.friotrackapi.auth.service;

import com.mt.friotrackapi.auth.dto.LoginRequest;
import com.mt.friotrackapi.auth.dto.LoginResponse;
import com.mt.friotrackapi.users.dto.UserResponse;
import com.mt.friotrackapi.users.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public LoginResponse login(LoginRequest request) {
        UserResponse user = userService.authenticate(request.username(), request.password());
        return new LoginResponse("demo-token-friotrack", "Bearer", user);
    }

    public UserResponse currentUser() {
        return userService.demoAdmin();
    }
}
