package com.mt.friotrackapi.alerts.controller;

import com.mt.friotrackapi.alerts.dto.AlertResponse;
import com.mt.friotrackapi.alerts.dto.AlertSummaryResponse;
import com.mt.friotrackapi.alerts.service.AlertService;
import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;
    private final TenantAccessService tenantAccessService;

    public AlertController(AlertService alertService, TenantAccessService tenantAccessService) {
        this.alertService = alertService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ApiResponse<List<AlertResponse>> findAll(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String vehicle,
            @RequestParam(required = false) String search
    ) {
        return ApiResponse.ok(alertService.findAll(tenantAccessService.companyId(), severity, status, type, vehicle, search));
    }

    @GetMapping("/summary")
    public ApiResponse<AlertSummaryResponse> summary() {
        return ApiResponse.ok(alertService.summary(tenantAccessService.companyId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<AlertResponse> findById(@PathVariable Long id) {
        AlertResponse alert = alertService.findById(id);
        tenantAccessService.requireCompany(alert.companyId());
        return ApiResponse.ok(alert);
    }

    @PatchMapping("/{id}/ack")
    public ApiResponse<AlertResponse> acknowledge(@PathVariable Long id) {
        tenantAccessService.requireCompany(alertService.findById(id).companyId());
        return ApiResponse.ok("Alerta reconocida", alertService.acknowledge(id));
    }

    @PatchMapping("/{id}/resolve")
    public ApiResponse<AlertResponse> resolve(@PathVariable Long id) {
        tenantAccessService.requireCompany(alertService.findById(id).companyId());
        return ApiResponse.ok("Alerta resuelta", alertService.resolve(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tenantAccessService.requireCompany(alertService.findById(id).companyId());
        alertService.delete(id);
        return ApiResponse.ok("Alerta eliminada", null);
    }
}
