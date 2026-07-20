package com.mt.friotrackapi.companies.controller;

import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.companies.dto.CompanyResponse;
import com.mt.friotrackapi.companies.dto.CreateCompanyRequest;
import com.mt.friotrackapi.companies.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        if (tenantAccessService.isServiceAdmin()) {
            return ApiResponse.ok(companyService.findAll());
        }
        return ApiResponse.ok(List.of(companyService.findById(tenantAccessService.companyId())));
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyResponse> findById(@PathVariable Long id) {
        tenantAccessService.requireCompany(id);
        return ApiResponse.ok(companyService.findById(id));
    }

    @PreAuthorize("hasRole('SA')")
    @PostMapping
    public ApiResponse<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
        return ApiResponse.ok("Empresa creada", companyService.create(request));
    }

    @PreAuthorize("hasRole('SA')")
    @PutMapping("/{id}")
    public ApiResponse<CompanyResponse> update(@PathVariable Long id, @Valid @RequestBody CreateCompanyRequest request) {
        return ApiResponse.ok("Empresa actualizada", companyService.update(id, request));
    }

    @PreAuthorize("hasRole('SA')")
    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<CompanyResponse> setStatus(@PathVariable Long id, @PathVariable String status) {
        return ApiResponse.ok("Estado de empresa actualizado", companyService.setStatus(id, status));
    }

    @PreAuthorize("hasRole('SA')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ApiResponse.ok("Empresa eliminada", null);
    }
}
