package com.mt.friotrackapi.vehicles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVehicleRequest(
        @NotNull Long companyId,
        @NotBlank String code,
        @NotBlank String plate,
        @NotBlank String label,
        @NotBlank String driver,
        @NotBlank String imei,
        @NotBlank String model,
        @NotNull Integer year,
        @NotBlank String unitType,
        @NotNull Integer loadCapacityKg
) {
}
