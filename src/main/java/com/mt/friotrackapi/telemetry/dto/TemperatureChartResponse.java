package com.mt.friotrackapi.telemetry.dto;

import java.util.List;

public record TemperatureChartResponse(
        List<TemperaturePointResponse> points,
        double minLimit,
        double maxLimit,
        String minLabel,
        String maxLabel,
        double chartMin,
        double chartMax,
        int tickStep
) {
}
