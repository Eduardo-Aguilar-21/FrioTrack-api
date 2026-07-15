package com.mt.friotrackapi.vehicles.controller;

import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.telemetry.dto.TelemetrySnapshotResponse;
import com.mt.friotrackapi.telemetry.dto.TemperaturePointResponse;
import com.mt.friotrackapi.telemetry.dto.VehicleEventResponse;
import com.mt.friotrackapi.telemetry.service.TelemetryService;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final TelemetryService telemetryService;

    public VehicleController(VehicleService vehicleService, TelemetryService telemetryService) {
        this.vehicleService = vehicleService;
        this.telemetryService = telemetryService;
    }

    @GetMapping
    public ApiResponse<List<VehicleResponse>> findAll(@RequestParam(required = false) Long companyId) {
        return ApiResponse.ok(vehicleService.findAll(companyId));
    }

    @GetMapping("/{id}")
    public ApiResponse<VehicleResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(vehicleService.findById(id));
    }

    @GetMapping("/{id}/telemetry")
    public ApiResponse<TelemetrySnapshotResponse> telemetry(@PathVariable Long id) {
        vehicleService.findById(id);
        return ApiResponse.ok(telemetryService.snapshot(id));
    }

    @GetMapping("/{id}/temperature-history")
    public ApiResponse<List<TemperaturePointResponse>> temperatureHistory(@PathVariable Long id) {
        vehicleService.findById(id);
        return ApiResponse.ok(telemetryService.temperatureHistory(id));
    }

    @GetMapping("/{id}/events")
    public ApiResponse<List<VehicleEventResponse>> events(@PathVariable Long id) {
        vehicleService.findById(id);
        return ApiResponse.ok(telemetryService.events(id));
    }
}
