package com.mt.friotrackapi.dashboard.dto;

public record FleetMapVehicleResponse(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        String status,
        String color,
        String temperature
) {
}
