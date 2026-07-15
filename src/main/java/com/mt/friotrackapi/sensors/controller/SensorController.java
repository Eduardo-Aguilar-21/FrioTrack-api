package com.mt.friotrackapi.sensors.controller;

import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.sensors.dto.CreateSensorRequest;
import com.mt.friotrackapi.sensors.dto.SensorResponse;
import com.mt.friotrackapi.sensors.service.SensorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping
    public ApiResponse<List<SensorResponse>> findAll(@RequestParam(required = false) Long companyId) {
        return ApiResponse.ok(sensorService.findAll(companyId));
    }

    @PostMapping
    public ApiResponse<SensorResponse> create(@Valid @RequestBody CreateSensorRequest request) {
        return ApiResponse.ok("Sensor creado", sensorService.create(request));
    }


    @PutMapping("/{id}")
    public ApiResponse<SensorResponse> update(@PathVariable Long id, @Valid @RequestBody CreateSensorRequest request) {
        return ApiResponse.ok("Sensor actualizado", sensorService.update(id, request));
    }
}
