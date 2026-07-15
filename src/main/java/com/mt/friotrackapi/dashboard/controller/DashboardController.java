package com.mt.friotrackapi.dashboard.controller;

import com.mt.friotrackapi.alerts.dto.AlertResponse;
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

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary(@RequestParam(defaultValue = "1") Long companyId) {
        return ApiResponse.ok(dashboardService.summary(companyId));
    }

    @GetMapping("/fleet-map")
    public ApiResponse<List<FleetMapVehicleResponse>> fleetMap(@RequestParam(defaultValue = "1") Long companyId) {
        return ApiResponse.ok(dashboardService.fleetMap(companyId));
    }

    @GetMapping("/vehicle-status")
    public ApiResponse<List<VehicleStatusResponse>> vehicleStatus(@RequestParam(defaultValue = "1") Long companyId) {
        return ApiResponse.ok(dashboardService.vehicleStatus(companyId));
    }

    @GetMapping("/temperature-distribution")
    public ApiResponse<TemperatureDistributionResponse> temperatureDistribution(@RequestParam(defaultValue = "1") Long companyId) {
        return ApiResponse.ok(dashboardService.temperatureDistribution(companyId));
    }

    @GetMapping("/recent-alerts")
    public ApiResponse<List<AlertResponse>> recentAlerts(@RequestParam(defaultValue = "1") Long companyId, @RequestParam(defaultValue = "4") int limit) {
        return ApiResponse.ok(dashboardService.recentAlerts(companyId, limit));
    }

    @GetMapping("/featured-vehicle")
    public ApiResponse<FeaturedVehicleResponse> featuredVehicle(@RequestParam(defaultValue = "1") Long companyId, @RequestParam(required = false) Long vehicleId) {
        return ApiResponse.ok(dashboardService.featuredVehicle(companyId, vehicleId));
    }
}
