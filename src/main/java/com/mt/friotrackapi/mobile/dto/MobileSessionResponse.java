package com.mt.friotrackapi.mobile.dto;

public record MobileSessionResponse(
        String token,
        Long companyId,
        String companyName,
        Long userId,
        String userName,
        String deviceName
) {
}
