package com.mt.friotrackapi.dashboard.dto;

public record TemperatureDistributionResponse(
        int inRange,
        int warning,
        int outOfRange,
        int offline,
        String targetRange
) {
}
