package com.mt.friotrackapi.auth.dto;

import com.mt.friotrackapi.users.dto.UserResponse;

public record LoginResponse(
        String token,
        String tokenType,
        UserResponse user
) {
}
