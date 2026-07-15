package com.mt.friotrackapi.vehicles.dto;

public record VehicleResponse(
        Long id,
        Long companyId,
        String code,
        String plate,
        String label,
        String status,
        String driver,
        String imei,
        String model,
        Integer year,
        String unitType,
        Integer loadCapacityKg,
        Double latitude,
        Double longitude,
        String currentTemperature,
        String temperatureState,
        String doorState,
        String coolingUnitState,
        String lastCommunication
) {
}
