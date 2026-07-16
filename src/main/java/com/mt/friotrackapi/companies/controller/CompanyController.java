package com.mt.friotrackapi.companies.controller;

import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.companies.dto.CompanyResponse;
import com.mt.friotrackapi.companies.service.CompanyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final TenantAccessService tenantAccessService;

    public CompanyController(CompanyService companyService, TenantAccessService tenantAccessService) {
        this.companyService = companyService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ApiResponse<List<CompanyResponse>> findAll() {
        return ApiResponse.ok(List.of(companyService.findById(tenantAccessService.companyId())));
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyResponse> findById(@PathVariable Long id) {
        tenantAccessService.requireCompany(id);
        return ApiResponse.ok(companyService.findById(id));
    }
}
