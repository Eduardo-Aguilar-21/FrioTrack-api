package com.mt.friotrackapi.protocol.dto;

import java.util.List;
import java.util.Map;

public record ProtocolConfigResponse(
        Long companyId,
        String protocol,
        String brokerName,
        String topicPattern,
        String topicExample,
        String payloadRoot,
        List<ProtocolFieldConfigResponse> fields,
        TemperatureRulesResponse temperatureRules,
        List<AdvancedAlertRuleResponse> advancedAlertRules,
        Map<String, Object> previewPayload
) {
}
