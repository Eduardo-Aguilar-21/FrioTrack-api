package com.mt.friotrackapi.mobile.dto;

import java.time.Instant;

public record MobileAccessCodeResponse(
        Long companyId,
        String code,
        Instant expiresAt
) {
}
