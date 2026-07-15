package com.mt.friotrackapi.alerts.controller;

import com.mt.friotrackapi.alerts.dto.AlertResponse;
import com.mt.friotrackapi.alerts.dto.AlertSummaryResponse;
import com.mt.friotrackapi.alerts.service.AlertService;
import com.mt.friotrackapi.common.response.ApiResponse;
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

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ApiResponse<List<AlertResponse>> findAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String severity
    ) {
        return ApiResponse.ok(alertService.findAll(companyId, severity));
    }

    @GetMapping("/summary")
    public ApiResponse<AlertSummaryResponse> summary() {
        return ApiResponse.ok(alertService.summary());
    }

    @GetMapping("/{id}")
    public ApiResponse<AlertResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(alertService.findById(id));
    }

    @PatchMapping("/{id}/resolve")
    public ApiResponse<AlertResponse> resolve(@PathVariable Long id) {
        return ApiResponse.ok("Alerta resuelta", alertService.resolve(id));
    }
}
