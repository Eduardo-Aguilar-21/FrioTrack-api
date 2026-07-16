package com.mt.friotrackapi.protocol.dto;

public record TemperatureRulesResponse(
        Double minAllowed,
        Double maxAllowed,
        Double criticalLow,
        Double criticalHigh
) {
}
