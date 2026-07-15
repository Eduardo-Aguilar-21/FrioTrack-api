package com.mt.friotrackapi.users.dto;

public record UserResponse(
        Long id,
        Long companyId,
        String companyName,
        String username,
        String name,
        String email,
        String role,
        String status
) {
}
