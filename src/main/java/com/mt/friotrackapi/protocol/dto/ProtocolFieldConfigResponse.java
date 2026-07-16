package com.mt.friotrackapi.protocol.dto;

public record ProtocolFieldConfigResponse(
        String key,
        String label,
        boolean enabled,
        String jsonPath,
        String dataType,
        String unit,
        String sampleValue,
        String targetField,
        Boolean required
) {
}
