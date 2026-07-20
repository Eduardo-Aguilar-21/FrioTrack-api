package com.mt.friotrackapi.notificationgroups.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateNotificationGroupRequest(
        @NotNull Long companyId,
        @NotBlank String name,
        String description,
        @NotNull List<String> alertTypes,
        @NotNull List<String> severities,
        @NotNull List<String> channels,
        List<Long> userIds,
        List<Long> vehicleIds,
        @NotBlank String status
) {
}
