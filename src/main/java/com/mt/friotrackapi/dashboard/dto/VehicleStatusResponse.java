package com.mt.friotrackapi.dashboard.dto;

public record VehicleStatusResponse(
        Long vehicleId,
        String vehicle,
        String status,
        String statusColorKey,
        String temperature,
        String rangeStatus,
        String rangeStatusColorKey,
        String doorState,
        String coolingUnitState,
        String lastCommunication
) {
}
