package com.mt.friotrackapi.telemetry.dto;

public record TemperaturePointResponse(
        String time,
        Double temperature
) {
}
