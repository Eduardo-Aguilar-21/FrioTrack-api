package com.mt.friotrackapi.companies.dto;

public record CompanyResponse(
        Long id,
        String name,
        String taxId,
        String status
) {
}
