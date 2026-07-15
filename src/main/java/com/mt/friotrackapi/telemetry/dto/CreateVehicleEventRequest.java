package com.mt.friotrackapi.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVehicleEventRequest(
        @NotNull Long vehicleId,
        @NotBlank String type,
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String time,
        @NotBlank String severity
) {
}
