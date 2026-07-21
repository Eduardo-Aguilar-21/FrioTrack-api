package com.mt.friotrackapi.protocol.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveProtocolConfigRequest(
        @NotNull Long companyId,
        @NotBlank String brokerName,
        @NotBlank String topicPattern,
        String payloadRoot,
        @Valid List<ProtocolFieldConfigResponse> fields,
        TemperatureRulesResponse temperatureRules,
        List<AdvancedAlertRuleResponse> advancedAlertRules
) {
}
