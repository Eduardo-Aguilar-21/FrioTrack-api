package com.mt.friotrackapi.protocol.dto;

public record AdvancedAlertRuleResponse(
        Boolean enabled,
        String name,
        String conditionField,
        String rangeCondition,
        Boolean requireMoving,
        Double minSpeed,
        Boolean requireIgnitionOn,
        Long stationaryDurationSeconds,
        Long persistenceSeconds
) {
}
