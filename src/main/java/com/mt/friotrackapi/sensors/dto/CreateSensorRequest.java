package com.mt.friotrackapi.sensors.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSensorRequest(
        @NotNull Long companyId,
        @NotNull Long vehicleId,
        @NotBlank String code,
        @NotBlank String type,
        @NotBlank String unit
) {
}
