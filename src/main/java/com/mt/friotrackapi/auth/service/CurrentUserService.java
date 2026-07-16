package com.mt.friotrackapi.auth.service;

import com.mt.friotrackapi.auth.dto.AuthenticatedUser;
import com.mt.friotrackapi.common.exception.AuthException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthException("Sesion requerida");
        }
        return user;
    }

    public Long companyId() {
        return currentUser().companyId();
    }
}
