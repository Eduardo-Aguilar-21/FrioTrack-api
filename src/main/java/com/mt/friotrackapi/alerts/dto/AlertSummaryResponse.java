package com.mt.friotrackapi.alerts.dto;

public record AlertSummaryResponse(
        int critical,
        int criticalPercent,
        int warnings,
        int warningsPercent,
        int informational,
        int informationalPercent,
        int offline,
        int offlinePercent,
        int totalActive
) {
}
