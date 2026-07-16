package com.mt.friotrackapi.auth.service;

import com.mt.friotrackapi.auth.dto.LoginRequest;
import com.mt.friotrackapi.auth.dto.LoginResponse;
import com.mt.friotrackapi.users.dto.UserResponse;
import com.mt.friotrackapi.users.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final CurrentUserService currentUserService;

    public AuthService(UserService userService, AuthTokenService authTokenService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.authTokenService = authTokenService;
        this.currentUserService = currentUserService;
    }

    public LoginResponse login(LoginRequest request) {
        UserResponse user = userService.authenticate(request.username(), request.password());
        return new LoginResponse(authTokenService.createToken(user), "Bearer", user);
    }

    public UserResponse currentUser() {
        return userService.findById(currentUserService.currentUser().id());
    }
}
