package com.mt.friotrackapi.telemetry.dto;

public record TelemetrySnapshotResponse(
        Long vehicleId,
        String temperature,
        String temperatureState,
        String humidity,
        String doorState,
        String coolingUnitState,
        String fuelLevel,
        String speed,
        String targetRange,
        Double latitude,
        Double longitude,
        String address,
        String lastCommunication
) {
}
