package com.mt.friotrackapi.sensors.controller;

import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.sensors.dto.CreateSensorRequest;
import com.mt.friotrackapi.sensors.dto.SensorResponse;
import com.mt.friotrackapi.sensors.service.SensorService;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorService sensorService;
    private final VehicleService vehicleService;
    private final TenantAccessService tenantAccessService;

    public SensorController(SensorService sensorService, VehicleService vehicleService, TenantAccessService tenantAccessService) {
        this.sensorService = sensorService;
        this.vehicleService = vehicleService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ApiResponse<List<SensorResponse>> findAll() {
        return ApiResponse.ok(sensorService.findAll(tenantAccessService.companyId()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PostMapping
    public ApiResponse<SensorResponse> create(@Valid @RequestBody CreateSensorRequest request) {
        return ApiResponse.ok("Sensor creado", sensorService.create(scopedRequest(request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PutMapping("/{id}")
    public ApiResponse<SensorResponse> update(@PathVariable Long id, @Valid @RequestBody CreateSensorRequest request) {
        SensorResponse sensor = sensorService.findById(id);
        tenantAccessService.requireCompany(sensor.companyId());
        return ApiResponse.ok("Sensor actualizado", sensorService.update(id, scopedRequest(request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<SensorResponse> setStatus(@PathVariable Long id, @PathVariable String status) {
        SensorResponse sensor = sensorService.findById(id);
        tenantAccessService.requireCompany(sensor.companyId());
        return ApiResponse.ok("Estado de sensor actualizado", sensorService.setStatus(id, status));
    }

    private CreateSensorRequest scopedRequest(CreateSensorRequest request) {
        VehicleResponse vehicle = vehicleService.findById(request.vehicleId());
        tenantAccessService.requireCompany(vehicle.companyId());
        return new CreateSensorRequest(tenantAccessService.companyId(), request.vehicleId(), request.code(), request.type(), request.unit());
    }
}
