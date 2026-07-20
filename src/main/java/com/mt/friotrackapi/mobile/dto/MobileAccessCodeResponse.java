package com.mt.friotrackapi.mobile.dto;

import java.time.Instant;

public record MobileAccessCodeResponse(
        Long companyId,
        Long userId,
        String userName,
        String code,
        Instant expiresAt
) {
}
