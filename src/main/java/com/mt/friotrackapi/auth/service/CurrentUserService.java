package com.mt.friotrackapi.auth.service;

import com.mt.friotrackapi.auth.dto.AuthenticatedUser;
import com.mt.friotrackapi.common.exception.AuthException;
import com.mt.friotrackapi.users.dto.UserResponse;
import com.mt.friotrackapi.users.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserService userService;

    public CurrentUserService(UserService userService) {
        this.userService = userService;
    }

    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthException("Sesion requerida");
        }

        UserResponse userEntity = userService.findById(user.id());
        if (!"ACTIVE".equalsIgnoreCase(userEntity.status())) {
            throw new AuthException("Usuario inactivo");
        }

        return new AuthenticatedUser(userEntity.id(), userEntity.companyId(), userEntity.username(), userEntity.role());
    }

    public Long companyId() {
        return currentUser().companyId();
    }
}
