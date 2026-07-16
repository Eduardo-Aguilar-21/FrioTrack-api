package com.mt.friotrackapi.sensors.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.sensors.dto.CreateSensorRequest;
import com.mt.friotrackapi.sensors.dto.SensorResponse;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SensorService {

    private final CompanyService companyService;
    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storePath = Path.of(System.getProperty("user.dir"), "data", "sensors.json");
    private final List<SensorResponse> sensors = new ArrayList<>();

    public SensorService(CompanyService companyService, VehicleService vehicleService) {
        this.companyService = companyService;
        this.vehicleService = vehicleService;
        loadSensors();
    }

    public List<SensorResponse> findAll(Long companyId) {
        if (companyId == null) {
            return sensors;
        }

        companyService.findById(companyId);
        return sensors.stream()
                .filter(sensor -> sensor.companyId().equals(companyId))
                .toList();
    }

    public SensorResponse findById(Long id) {
        return sensors.stream()
                .filter(sensor -> sensor.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException("Sensor no encontrado"));
    }

    public SensorResponse create(CreateSensorRequest request) {
        companyService.findById(request.companyId());
        VehicleResponse vehicle = vehicleService.findById(request.vehicleId());
        if (!vehicle.companyId().equals(request.companyId())) {
            throw new ApiException("El vehiculo no pertenece a la empresa");
        }

        boolean exists = sensors.stream()
                .anyMatch(sensor -> sensor.companyId().equals(request.companyId()) && sensor.code().equalsIgnoreCase(request.code()));
        if (exists) {
            throw new ApiException("El sensor ya existe en la empresa");
        }

        SensorResponse sensor = new SensorResponse(
                nextId(),
                request.companyId(),
                request.vehicleId(),
                request.code(),
                vehicle.label(),
                request.type(),
                request.unit(),
                "Sin lectura",
                "ACTIVE"
        );
        sensors.add(sensor);
        saveSensors();
        return sensor;
    }


    public SensorResponse update(Long id, CreateSensorRequest request) {
        SensorResponse current = findById(id);
        companyService.findById(request.companyId());
        VehicleResponse vehicle = vehicleService.findById(request.vehicleId());
        if (!vehicle.companyId().equals(request.companyId())) {
            throw new ApiException("El vehiculo no pertenece a la empresa");
        }

        boolean exists = sensors.stream()
                .anyMatch(sensor -> !sensor.id().equals(id)
                        && sensor.companyId().equals(request.companyId())
                        && sensor.code().equalsIgnoreCase(request.code()));
        if (exists) {
            throw new ApiException("El sensor ya existe en la empresa");
        }

        SensorResponse updated = new SensorResponse(
                current.id(),
                request.companyId(),
                request.vehicleId(),
                request.code(),
                vehicle.label(),
                request.type(),
                request.unit(),
                current.lastValue(),
                current.status()
        );
        sensors.set(sensors.indexOf(current), updated);
        saveSensors();
        return updated;
    }

    private Long nextId() {
        return sensors.stream().mapToLong(SensorResponse::id).max().orElse(0L) + 1;
    }

    private void loadSensors() {
        try {
            if (Files.exists(storePath)) {
                sensors.addAll(objectMapper.readValue(storePath.toFile(), new TypeReference<List<SensorResponse>>() {}));
                return;
            }

            sensors.addAll(defaultSensors());
            saveSensors();
        } catch (IOException ex) {
            throw new ApiException("No se pudo cargar sensores persistidos");
        }
    }

    private void saveSensors() {
        try {
            Files.createDirectories(storePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), sensors);
        } catch (IOException ex) {
            throw new ApiException("No se pudo persistir sensores");
        }
    }

    private List<SensorResponse> defaultSensors() {
        return List.of(
                new SensorResponse(1L, 1L, 12L, "TMP-001", "Camion 12 - ABC123", "Temperatura", "°C", "9.8 °C", "ACTIVE"),
                new SensorResponse(2L, 1L, 12L, "HUM-014", "Camion 12 - ABC123", "Humedad", "%", "45 %", "ACTIVE"),
                new SensorResponse(3L, 1L, 7L, "DR-032", "Camion 07 - DEF456", "Puerta", "estado", "Abierta", "ACTIVE")
        );
    }
}
