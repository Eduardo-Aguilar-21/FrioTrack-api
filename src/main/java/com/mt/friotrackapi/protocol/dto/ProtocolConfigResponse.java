package com.mt.friotrackapi.protocol.dto;

import java.util.List;
import java.util.Map;

public record ProtocolConfigResponse(
        Long companyId,
        String brokerName,
        String topicPattern,
        String topicExample,
        String payloadRoot,
        List<ProtocolFieldConfigResponse> fields,
        TemperatureRulesResponse temperatureRules,
        Map<String, Object> previewPayload
) {
}
