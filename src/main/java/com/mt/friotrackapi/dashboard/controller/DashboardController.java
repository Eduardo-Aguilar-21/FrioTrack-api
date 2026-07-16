package com.mt.friotrackapi.dashboard.controller;

import com.mt.friotrackapi.alerts.dto.AlertResponse;
import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.dashboard.dto.DashboardSummaryResponse;
import com.mt.friotrackapi.dashboard.dto.FeaturedVehicleResponse;
import com.mt.friotrackapi.dashboard.dto.FleetMapVehicleResponse;
import com.mt.friotrackapi.dashboard.dto.TemperatureDistributionResponse;
import com.mt.friotrackapi.dashboard.dto.VehicleStatusResponse;
import com.mt.friotrackapi.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final TenantAccessService tenantAccessService;

    public DashboardController(DashboardService dashboardService, TenantAccessService tenantAccessService) {
        this.dashboardService = dashboardService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.ok(dashboardService.summary(tenantAccessService.companyId()));
    }

    @GetMapping("/fleet-map")
    public ApiResponse<List<FleetMapVehicleResponse>> fleetMap() {
        return ApiResponse.ok(dashboardService.fleetMap(tenantAccessService.companyId()));
    }

    @GetMapping("/vehicle-status")
    public ApiResponse<List<VehicleStatusResponse>> vehicleStatus() {
        return ApiResponse.ok(dashboardService.vehicleStatus(tenantAccessService.companyId()));
    }

    @GetMapping("/temperature-distribution")
    public ApiResponse<TemperatureDistributionResponse> temperatureDistribution() {
        return ApiResponse.ok(dashboardService.temperatureDistribution(tenantAccessService.companyId()));
    }

    @GetMapping("/recent-alerts")
    public ApiResponse<List<AlertResponse>> recentAlerts(@RequestParam(defaultValue = "4") int limit) {
        return ApiResponse.ok(dashboardService.recentAlerts(tenantAccessService.companyId(), limit));
    }

    @GetMapping("/featured-vehicle")
    public ApiResponse<FeaturedVehicleResponse> featuredVehicle(@RequestParam(required = false) Long vehicleId) {
        return ApiResponse.ok(dashboardService.featuredVehicle(tenantAccessService.companyId(), vehicleId));
    }
}
