package com.mt.friotrackapi.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTelemetrySnapshotRequest(
        @NotNull Long vehicleId,
        @NotBlank String temperature,
        @NotBlank String temperatureState,
        @NotBlank String humidity,
        @NotBlank String doorState,
        @NotBlank String coolingUnitState,
        @NotBlank String fuelLevel,
        @NotBlank String speed,
        @NotBlank String targetRange,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank String address,
        @NotBlank String lastCommunication
) {
}
