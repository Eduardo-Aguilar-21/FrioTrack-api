package com.mt.friotrackapi.tracking.controller;

import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.common.dto.PageResponse;
import com.mt.friotrackapi.tracking.dto.TripResponse;
import com.mt.friotrackapi.tracking.service.TrackingService;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {
    private final TrackingService trackingService;
    private final VehicleService vehicleService;
    private final TenantAccessService tenantAccessService;

    public TrackingController(TrackingService trackingService, VehicleService vehicleService, TenantAccessService tenantAccessService) {
        this.trackingService = trackingService;
        this.vehicleService = vehicleService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/vehicles/{vehicleId}/trips")
    public ApiResponse<List<TripResponse>> trips(@PathVariable Long vehicleId, @RequestParam(defaultValue = "7") int days) {
        requireVehicle(vehicleId);
        return ApiResponse.ok(trackingService.trips(vehicleId, days));
    }

    @GetMapping("/vehicles/{vehicleId}/trips/paged")
    public ApiResponse<PageResponse<TripResponse>> tripPage(
            @PathVariable Long vehicleId, @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        requireVehicle(vehicleId);
        return ApiResponse.ok(trackingService.tripPage(vehicleId, days, page, size));
    }

    @GetMapping("/vehicles/{vehicleId}/trips/export")
    public ResponseEntity<byte[]> export(@PathVariable Long vehicleId, @RequestParam(defaultValue = "7") int days) {
        requireVehicle(vehicleId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=seguimiento-vehiculo-" + vehicleId + ".csv")
                .contentType(new MediaType("text", "csv"))
                .body(trackingService.exportCsv(vehicleId, days));
    }

    private void requireVehicle(Long vehicleId) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        tenantAccessService.requireCompany(vehicle.companyId());
    }
}
