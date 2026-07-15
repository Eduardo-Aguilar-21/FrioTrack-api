package com.mt.friotrackapi.dashboard.dto;

import com.mt.friotrackapi.telemetry.dto.TelemetrySnapshotResponse;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;

public record FeaturedVehicleResponse(
        VehicleResponse vehicle,
        TelemetrySnapshotResponse telemetry
) {
}
