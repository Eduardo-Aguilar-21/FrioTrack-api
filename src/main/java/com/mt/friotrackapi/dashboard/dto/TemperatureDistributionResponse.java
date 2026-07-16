package com.mt.friotrackapi.dashboard.dto;

public record TemperatureDistributionResponse(
        int totalVehicles,
        int inRange,
        int inRangePercent,
        int warning,
        int warningPercent,
        int outOfRange,
        int outOfRangePercent,
        int offline,
        int offlinePercent,
        String targetRange
) {
}
