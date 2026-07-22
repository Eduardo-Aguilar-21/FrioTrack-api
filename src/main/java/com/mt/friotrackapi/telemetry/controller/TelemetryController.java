package com.mt.friotrackapi.telemetry.controller;

import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.common.dto.PageResponse;
import com.mt.friotrackapi.telemetry.dto.CreateVehicleEventRequest;
import com.mt.friotrackapi.telemetry.dto.SaveTemperatureHistoryRequest;
import com.mt.friotrackapi.telemetry.dto.TelemetrySnapshotResponse;
import com.mt.friotrackapi.telemetry.dto.TelemetryReadingResponse;
import com.mt.friotrackapi.telemetry.dto.TemperatureChartResponse;
import com.mt.friotrackapi.telemetry.dto.TemperaturePointResponse;
import com.mt.friotrackapi.telemetry.dto.UpdateTelemetrySnapshotRequest;
import com.mt.friotrackapi.telemetry.dto.VehicleDailySummaryResponse;
import com.mt.friotrackapi.telemetry.dto.VehicleEventResponse;
import com.mt.friotrackapi.telemetry.service.TelemetryService;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;
    private final VehicleService vehicleService;
    private final TenantAccessService tenantAccessService;

    public TelemetryController(TelemetryService telemetryService, VehicleService vehicleService, TenantAccessService tenantAccessService) {
        this.telemetryService = telemetryService;
        this.vehicleService = vehicleService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/vehicles/{vehicleId}/latest")
    public ApiResponse<TelemetrySnapshotResponse> latest(@PathVariable Long vehicleId) {
        requireVehicle(vehicleId);
        return ApiResponse.ok(telemetryService.snapshot(vehicleId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PutMapping("/vehicles/{vehicleId}/latest")
    public ApiResponse<TelemetrySnapshotResponse> updateLatest(@PathVariable Long vehicleId, @Valid @RequestBody UpdateTelemetrySnapshotRequest request) {
        requireVehicle(vehicleId);
        UpdateTelemetrySnapshotRequest normalized = new UpdateTelemetrySnapshotRequest(
                vehicleId,
                request.temperature(),
                request.temperatureState(),
                request.humidity(),
                request.doorState(),
                request.coolingUnitState(),
                request.fuelLevel(),
                request.speed(),
                request.targetRange(),
                request.latitude(),
                request.longitude(),
                request.address(),
                request.lastCommunication()
        );
        return ApiResponse.ok("Telemetria actualizada", telemetryService.updateSnapshot(normalized));
    }

    @GetMapping("/vehicles/{vehicleId}/temperature-history")
    public ApiResponse<List<TemperaturePointResponse>> temperatureHistory(@PathVariable Long vehicleId) {
        requireVehicle(vehicleId);
        return ApiResponse.ok(telemetryService.temperatureHistory(vehicleId));
    }

    @GetMapping("/vehicles/{vehicleId}/temperature-chart")
    public ApiResponse<TemperatureChartResponse> temperatureChart(@PathVariable Long vehicleId) {
        requireVehicle(vehicleId);
        return ApiResponse.ok(telemetryService.temperatureChart(vehicleId));
    }

    @GetMapping("/vehicles/{vehicleId}/daily-summary")
    public ApiResponse<VehicleDailySummaryResponse> dailySummary(@PathVariable Long vehicleId) {
        requireVehicle(vehicleId);
        return ApiResponse.ok(telemetryService.dailySummary(vehicleId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PutMapping("/vehicles/{vehicleId}/temperature-history")
    public ApiResponse<List<TemperaturePointResponse>> saveTemperatureHistory(@PathVariable Long vehicleId, @Valid @RequestBody SaveTemperatureHistoryRequest request) {
        requireVehicle(vehicleId);
        SaveTemperatureHistoryRequest normalized = new SaveTemperatureHistoryRequest(vehicleId, request.points());
        return ApiResponse.ok("Historial actualizado", telemetryService.saveTemperatureHistory(normalized));
    }


    @GetMapping("/vehicles/{vehicleId}/readings/paged")
    public ApiResponse<PageResponse<TelemetryReadingResponse>> readingPage(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        requireVehicle(vehicleId);
        return ApiResponse.ok(telemetryService.readingPage(vehicleId, days, page, size));
    }

    @GetMapping("/vehicles/{vehicleId}/events")
    public ApiResponse<List<VehicleEventResponse>> events(@PathVariable Long vehicleId) {
        requireVehicle(vehicleId);
        return ApiResponse.ok(telemetryService.events(vehicleId));
    }

    @GetMapping("/vehicles/{vehicleId}/events/paged")
    public ApiResponse<PageResponse<VehicleEventResponse>> eventPage(
            @PathVariable Long vehicleId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        requireVehicle(vehicleId);
        return ApiResponse.ok(telemetryService.eventPage(vehicleId, page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PostMapping("/vehicles/{vehicleId}/events")
    public ApiResponse<VehicleEventResponse> addEvent(@PathVariable Long vehicleId, @Valid @RequestBody CreateVehicleEventRequest request) {
        requireVehicle(vehicleId);
        CreateVehicleEventRequest normalized = new CreateVehicleEventRequest(
                vehicleId,
                request.type(),
                request.title(),
                request.description(),
                request.time(),
                request.severity()
        );
        return ApiResponse.ok("Evento creado", telemetryService.addEvent(normalized));
    }

    private void requireVehicle(Long vehicleId) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        tenantAccessService.requireCompany(vehicle.companyId());
    }
}
