package com.mt.friotrackapi.alerts.dto;

public record AlertSummaryResponse(
        int critical,
        int warnings,
        int informational,
        int offline,
        int totalActive
) {
}
