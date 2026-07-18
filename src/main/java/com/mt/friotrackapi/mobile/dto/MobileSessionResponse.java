package com.mt.friotrackapi.mobile.dto;

public record MobileSessionResponse(
        String token,
        Long companyId,
        String companyName,
        String deviceName
) {
}
