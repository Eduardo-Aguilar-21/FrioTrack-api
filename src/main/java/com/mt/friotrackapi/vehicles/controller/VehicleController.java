package com.mt.friotrackapi.vehicles.controller;

import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.telemetry.dto.TelemetrySnapshotResponse;
import com.mt.friotrackapi.telemetry.dto.TemperaturePointResponse;
import com.mt.friotrackapi.telemetry.dto.VehicleEventResponse;
import com.mt.friotrackapi.telemetry.service.TelemetryService;
import com.mt.friotrackapi.vehicles.dto.CreateVehicleRequest;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final TelemetryService telemetryService;
    private final TenantAccessService tenantAccessService;

    public VehicleController(VehicleService vehicleService, TelemetryService telemetryService, TenantAccessService tenantAccessService) {
        this.vehicleService = vehicleService;
        this.telemetryService = telemetryService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ApiResponse<List<VehicleResponse>> findAll() {
        return ApiResponse.ok(vehicleService.findAll(tenantAccessService.companyId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<VehicleResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(requireVehicle(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
        CreateVehicleRequest scoped = scopedRequest(request);
        return ApiResponse.ok("Vehiculo creado", vehicleService.create(scoped));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<VehicleResponse> update(@PathVariable Long id, @Valid @RequestBody CreateVehicleRequest request) {
        requireVehicle(id);
        return ApiResponse.ok("Vehiculo actualizado", vehicleService.update(id, scopedRequest(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<VehicleResponse> setStatus(@PathVariable Long id, @PathVariable String status) {
        requireVehicle(id);
        return ApiResponse.ok("Estado de vehiculo actualizado", vehicleService.setStatus(id, status));
    }

    @GetMapping("/{id}/telemetry")
    public ApiResponse<TelemetrySnapshotResponse> telemetry(@PathVariable Long id) {
        requireVehicle(id);
        return ApiResponse.ok(telemetryService.snapshot(id));
    }

    @GetMapping("/{id}/temperature-history")
    public ApiResponse<List<TemperaturePointResponse>> temperatureHistory(@PathVariable Long id) {
        requireVehicle(id);
        return ApiResponse.ok(telemetryService.temperatureHistory(id));
    }

    @GetMapping("/{id}/events")
    public ApiResponse<List<VehicleEventResponse>> events(@PathVariable Long id) {
        requireVehicle(id);
        return ApiResponse.ok(telemetryService.events(id));
    }

    private VehicleResponse requireVehicle(Long id) {
        VehicleResponse vehicle = vehicleService.findById(id);
        tenantAccessService.requireCompany(vehicle.companyId());
        return vehicle;
    }

    private CreateVehicleRequest scopedRequest(CreateVehicleRequest request) {
        return new CreateVehicleRequest(tenantAccessService.companyId(), request.code(), request.plate(), request.label(), request.driver(), request.imei(), request.model(), request.year(), request.unitType(), request.loadCapacityKg());
    }
}
