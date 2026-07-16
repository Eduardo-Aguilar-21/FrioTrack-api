package com.mt.friotrackapi.auth.dto;

public record AuthenticatedUser(
        Long id,
        Long companyId,
        String username,
        String role
) {
}
