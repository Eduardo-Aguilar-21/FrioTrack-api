package com.mt.friotrackapi.telemetry.dto;

public record VehicleEventResponse(
        String type,
        String title,
        String description,
        String time,
        String severity
) {
}
