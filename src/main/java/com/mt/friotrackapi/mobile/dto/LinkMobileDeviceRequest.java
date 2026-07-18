package com.mt.friotrackapi.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkMobileDeviceRequest(
        @NotBlank
        @Size(min = 4, max = 6)
        String code,

        String deviceName,

        String pushToken
) {
}
