package com.mt.friotrackapi.companies.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCompanyRequest(
        @NotBlank String name,
        @NotBlank String taxId,
        @NotBlank String status,
        @NotNull @Min(1) Integer warningOfflineMinutes,
        @NotNull @Min(1) Integer criticalOfflineMinutes
) {
}
