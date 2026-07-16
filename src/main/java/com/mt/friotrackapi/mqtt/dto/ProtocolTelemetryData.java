package com.mt.friotrackapi.mqtt.dto;

import java.util.List;
import java.util.Map;

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
        Map<String, Object> customFields,
        List<String> errors
) {
}
