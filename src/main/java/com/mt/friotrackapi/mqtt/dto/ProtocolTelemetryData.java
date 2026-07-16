package com.mt.friotrackapi.mqtt.dto;

import java.util.List;

public record ProtocolTelemetryData(
        String temperature,
        Double temperatureValue,
        String temperatureState,
        String humidity,
        String doorState,
        String coolingUnitState,
        String fuelLevel,
        String speed,
        Double latitude,
        Double longitude,
        List<String> errors
) {
}
