package com.mt.friotrackapi.telemetry.controller;

import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.telemetry.dto.TelemetrySnapshotResponse;
import com.mt.friotrackapi.telemetry.service.TelemetryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
