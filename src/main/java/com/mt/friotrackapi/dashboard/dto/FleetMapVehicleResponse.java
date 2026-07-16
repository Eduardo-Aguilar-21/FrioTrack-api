package com.mt.friotrackapi.dashboard.dto;

public record FleetMapVehicleResponse(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        Double mapCenterLatitude,
        Double mapCenterLongitude,
        double mapZoom,
        String status,
        String statusLabel,
        String color,
        String temperature
) {
}
