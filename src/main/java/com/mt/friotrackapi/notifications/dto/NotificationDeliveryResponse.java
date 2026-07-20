package com.mt.friotrackapi.notifications.dto;

import java.time.Instant;

public record NotificationDeliveryResponse(
        Long id,
        Long companyId,
        Long alertId,
        String alertTitle,
        String alertType,
        String alertSeverity,
        Long groupId,
        String groupName,
        Long userId,
        String userName,
        String channel,
        String status,
        Instant deliveredAt,
        Instant readAt,
        String failureReason,
        Instant createdAt
) {
}
