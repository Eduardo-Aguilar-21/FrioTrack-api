package com.mt.friotrackapi.companies.controller;

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

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public ApiResponse<List<CompanyResponse>> findAll() {
        return ApiResponse.ok(companyService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(companyService.findById(id));
    }
}
