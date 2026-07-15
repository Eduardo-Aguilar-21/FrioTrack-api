package com.mt.friotrackapi.sensors.dto;

public record SensorResponse(
        Long id,
        Long companyId,
        Long vehicleId,
        String code,
        String vehicleLabel,
        String type,
        String unit,
        String lastValue,
        String status
) {
}
