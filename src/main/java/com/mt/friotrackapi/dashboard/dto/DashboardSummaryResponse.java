package com.mt.friotrackapi.dashboard.dto;

public record DashboardSummaryResponse(
        int totalVehicles,
        int inRange,
        int warning,
        int outOfRange,
        int offline,
        String averageTemperature
) {
}
