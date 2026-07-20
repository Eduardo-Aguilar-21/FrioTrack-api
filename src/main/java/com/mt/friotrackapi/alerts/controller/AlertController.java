package com.mt.friotrackapi.alerts.controller;

import com.mt.friotrackapi.alerts.dto.AlertResponse;
import com.mt.friotrackapi.alerts.dto.AlertSummaryResponse;
import com.mt.friotrackapi.alerts.service.AlertService;
import com.mt.friotrackapi.auth.service.CurrentUserService;
import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.notifications.service.NotificationDeliveryService;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final CurrentUserService currentUserService;
    private final NotificationDeliveryService notificationDeliveryService;

    public AlertController(AlertService alertService, TenantAccessService tenantAccessService, CurrentUserService currentUserService, NotificationDeliveryService notificationDeliveryService) {
        this.alertService = alertService;
        this.tenantAccessService = tenantAccessService;
        this.currentUserService = currentUserService;
        this.notificationDeliveryService = notificationDeliveryService;
    }

    @GetMapping
    public ApiResponse<List<AlertResponse>> findAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String vehicle,
            @RequestParam(required = false) String search
    ) {
        return ApiResponse.ok(alertService.findAll(tenantAccessService.resolveCompanyId(companyId), severity, status, type, vehicle, search));
    }

    @GetMapping("/summary")
    public ApiResponse<AlertSummaryResponse> summary(@RequestParam(required = false) Long companyId) {
        return ApiResponse.ok(alertService.summary(tenantAccessService.resolveCompanyId(companyId)));
    }

    @GetMapping("/{id}")
    public ApiResponse<AlertResponse> findById(@PathVariable Long id) {
        AlertResponse alert = alertService.findById(id);
        tenantAccessService.requireCompany(alert.companyId());
        return ApiResponse.ok(alert);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'SA')")
    @PatchMapping("/{id}/ack")
    public ApiResponse<AlertResponse> acknowledge(@PathVariable Long id) {
        tenantAccessService.requireCompany(alertService.findById(id).companyId());
        AlertResponse response = alertService.acknowledge(id);
        notificationDeliveryService.markRead(id, currentUserService.currentUser().id());
        return ApiResponse.ok("Alerta revisada", response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'SA')")
    @PatchMapping("/{id}/resolve")
    public ApiResponse<AlertResponse> resolve(@PathVariable Long id) {
        tenantAccessService.requireCompany(alertService.findById(id).companyId());
        return ApiResponse.ok("Alerta resuelta", alertService.resolve(id));
    }

    @PreAuthorize("hasRole('SA')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tenantAccessService.requireCompany(alertService.findById(id).companyId());
        alertService.delete(id);
        return ApiResponse.ok("Alerta eliminada", null);
    }
}
