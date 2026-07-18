package com.mt.friotrackapi.mobile.controller;

import com.mt.friotrackapi.alerts.dto.AlertResponse;
import com.mt.friotrackapi.alerts.dto.AlertSummaryResponse;
import com.mt.friotrackapi.alerts.service.AlertService;
import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.mobile.dto.CreateMobileAccessCodeRequest;
import com.mt.friotrackapi.mobile.dto.LinkMobileDeviceRequest;
import com.mt.friotrackapi.mobile.dto.MobileAccessCodeResponse;
import com.mt.friotrackapi.mobile.dto.MobileSessionResponse;
import com.mt.friotrackapi.mobile.service.MobileDeviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile")
public class MobileController {

    private final MobileDeviceService mobileDeviceService;
    private final AlertService alertService;
    private final TenantAccessService tenantAccessService;

    public MobileController(MobileDeviceService mobileDeviceService, AlertService alertService, TenantAccessService tenantAccessService) {
        this.mobileDeviceService = mobileDeviceService;
        this.alertService = alertService;
        this.tenantAccessService = tenantAccessService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @PostMapping("/access-code")
    public ApiResponse<MobileAccessCodeResponse> createAccessCode(@RequestBody(required = false) CreateMobileAccessCodeRequest request) {
        return ApiResponse.ok("Codigo movil generado", mobileDeviceService.createAccessCode(tenantAccessService.companyId(), request));
    }

    @PostMapping("/link")
    public ApiResponse<MobileSessionResponse> linkDevice(@Valid @RequestBody LinkMobileDeviceRequest request) {
        return ApiResponse.ok("Dispositivo vinculado", mobileDeviceService.linkDevice(request));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertResponse>> alerts(
            HttpServletRequest request,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String vehicle,
            @RequestParam(required = false) String search
    ) {
        Long companyId = mobileDeviceService.authenticate(request).companyId();
        return ApiResponse.ok(alertService.findAll(companyId, severity, status, type, vehicle, search));
    }

    @GetMapping("/alerts/summary")
    public ApiResponse<AlertSummaryResponse> alertSummary(HttpServletRequest request) {
        Long companyId = mobileDeviceService.authenticate(request).companyId();
        return ApiResponse.ok(alertService.summary(companyId));
    }

    @GetMapping("/alerts/{id}")
    public ApiResponse<AlertResponse> alertById(HttpServletRequest request, @PathVariable Long id) {
        Long companyId = mobileDeviceService.authenticate(request).companyId();
        AlertResponse alert = alertService.findById(id);
        requireMobileCompany(companyId, alert);
        return ApiResponse.ok(alert);
    }

    @PatchMapping("/alerts/{id}/review")
    public ApiResponse<AlertResponse> reviewAlert(HttpServletRequest request, @PathVariable Long id) {
        Long companyId = mobileDeviceService.authenticate(request).companyId();
        AlertResponse alert = alertService.findById(id);
        requireMobileCompany(companyId, alert);
        return ApiResponse.ok("Alerta revisada", alertService.acknowledge(id));
    }

    private void requireMobileCompany(Long companyId, AlertResponse alert) {
        if (!alert.companyId().equals(companyId)) {
            throw new com.mt.friotrackapi.common.exception.ForbiddenException("No puedes acceder a esta alerta");
        }
    }
}
