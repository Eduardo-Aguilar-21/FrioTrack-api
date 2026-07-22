package com.mt.friotrackapi.telemetry.dto;

import java.time.Instant;

public record TelemetryReadingResponse(
        Long id,
        Instant recordedAt,
        Double latitude,
        Double longitude,
        Double temperature,
        String humidity,
        String doorState,
        String coolingUnitState,
        String speed,
        String fuelLevel,
        String customFields
) {}
