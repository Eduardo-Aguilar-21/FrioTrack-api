package com.mt.friotrackapi.tracking.dto;

import java.util.Map;

public record TripReadingResponse(
        String recordedAt,
        String time,
        Double temperature,
        String humidity,
        String doorState,
        String coolingUnitState,
        String speed,
        String fuelLevel,
        Boolean ignitionOn,
        Double latitude,
        Double longitude,
        boolean outOfRange,
        Map<String, Object> sensorValues
) {}
