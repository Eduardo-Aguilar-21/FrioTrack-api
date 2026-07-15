package com.mt.friotrackapi.alerts.dto;

public record AlertResponse(
        Long id,
        Long companyId,
        String type,
        String severity,
        String title,
        String description,
        String vehicleLabel,
        String vehicleCode,
        String occurredAtLabel,
        String status,
        String duration
) {
}
