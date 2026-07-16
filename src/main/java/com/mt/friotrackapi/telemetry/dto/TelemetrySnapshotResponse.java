package com.mt.friotrackapi.telemetry.dto;

import java.util.Map;

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
        String lastCommunication,
        Map<String, Object> customFields
) {
}
