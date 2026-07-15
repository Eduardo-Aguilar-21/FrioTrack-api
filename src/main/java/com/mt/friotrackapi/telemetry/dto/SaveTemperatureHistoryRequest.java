package com.mt.friotrackapi.telemetry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveTemperatureHistoryRequest(
        @NotNull Long vehicleId,
        @NotEmpty List<@Valid TemperaturePointResponse> points
) {
}
