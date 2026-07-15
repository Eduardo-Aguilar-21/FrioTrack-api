package com.mt.friotrackapi.telemetry.controller;

import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.telemetry.dto.CreateVehicleEventRequest;
import com.mt.friotrackapi.telemetry.dto.SaveTemperatureHistoryRequest;
import com.mt.friotrackapi.telemetry.dto.TelemetrySnapshotResponse;
import com.mt.friotrackapi.telemetry.dto.TemperaturePointResponse;
import com.mt.friotrackapi.telemetry.dto.UpdateTelemetrySnapshotRequest;
import com.mt.friotrackapi.telemetry.dto.VehicleEventResponse;
import com.mt.friotrackapi.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/vehicles/{vehicleId}/latest")
    public ApiResponse<TelemetrySnapshotResponse> latest(@PathVariable Long vehicleId) {
        return ApiResponse.ok(telemetryService.snapshot(vehicleId));
    }

    @PutMapping("/vehicles/{vehicleId}/latest")
    public ApiResponse<TelemetrySnapshotResponse> updateLatest(@PathVariable Long vehicleId, @Valid @RequestBody UpdateTelemetrySnapshotRequest request) {
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
        return ApiResponse.ok(telemetryService.temperatureHistory(vehicleId));
    }

    @PutMapping("/vehicles/{vehicleId}/temperature-history")
    public ApiResponse<List<TemperaturePointResponse>> saveTemperatureHistory(@PathVariable Long vehicleId, @Valid @RequestBody SaveTemperatureHistoryRequest request) {
        SaveTemperatureHistoryRequest normalized = new SaveTemperatureHistoryRequest(vehicleId, request.points());
        return ApiResponse.ok("Historial actualizado", telemetryService.saveTemperatureHistory(normalized));
    }

    @GetMapping("/vehicles/{vehicleId}/events")
    public ApiResponse<List<VehicleEventResponse>> events(@PathVariable Long vehicleId) {
        return ApiResponse.ok(telemetryService.events(vehicleId));
    }

    @PostMapping("/vehicles/{vehicleId}/events")
    public ApiResponse<VehicleEventResponse> addEvent(@PathVariable Long vehicleId, @Valid @RequestBody CreateVehicleEventRequest request) {
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
}
