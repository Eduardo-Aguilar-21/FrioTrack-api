package com.mt.friotrackapi.telemetry.dto;

public record VehicleDailySummaryResponse(
        int inRangePercent,
        String inRangeDetail,
        int outOfRangePercent,
        String outOfRangeDetail,
        int doorOpenings,
        String doorOpeningsDetail,
        String averageTemperature,
        String averageTemperatureDetail
) {
}
