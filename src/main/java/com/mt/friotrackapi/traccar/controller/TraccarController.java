package com.mt.friotrackapi.traccar.controller;

import com.mt.friotrackapi.traccar.dto.TraccarStatusResponse;
import com.mt.friotrackapi.traccar.service.TraccarIntegrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traccar")
public class TraccarController {
    private final TraccarIntegrationService traccarIntegrationService;

    public TraccarController(TraccarIntegrationService traccarIntegrationService) {
        this.traccarIntegrationService = traccarIntegrationService;
    }

    @GetMapping("/status")
    public TraccarStatusResponse status(@RequestParam(required = false) Long companyId) {
        return traccarIntegrationService.status(companyId);
    }
}
