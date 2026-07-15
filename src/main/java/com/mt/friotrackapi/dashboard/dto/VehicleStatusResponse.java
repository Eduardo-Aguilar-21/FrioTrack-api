package com.mt.friotrackapi.dashboard.dto;

public record VehicleStatusResponse(
        Long vehicleId,
        String vehicle,
        String status,
        String temperature,
        String rangeStatus,
        String doorState,
        String coolingUnitState,
        String lastCommunication
) {
}
