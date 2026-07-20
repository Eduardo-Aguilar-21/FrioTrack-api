package com.mt.friotrackapi.notificationgroups.dto;

import java.util.List;

public record NotificationGroupResponse(
        Long id,
        Long companyId,
        String companyName,
        String name,
        String description,
        List<String> alertTypes,
        List<String> severities,
        List<String> channels,
        List<Long> userIds,
        List<String> userNames,
        List<Long> vehicleIds,
        List<String> vehicleLabels,
        String status
) {
}
