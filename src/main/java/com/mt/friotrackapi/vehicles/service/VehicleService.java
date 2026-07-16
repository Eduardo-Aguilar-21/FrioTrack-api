package com.mt.friotrackapi.vehicles.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.vehicles.dto.CreateVehicleRequest;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class VehicleService {

    private final CompanyService companyService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storePath = Path.of(System.getProperty("user.dir"), "data", "vehicles.json");
    private final List<VehicleResponse> vehicles = new ArrayList<>();

    public VehicleService(CompanyService companyService) {
        this.companyService = companyService;
        loadVehicles();
    }

    public List<VehicleResponse> findAll(Long companyId) {
        if (companyId == null) {
            return vehicles;
        }

        companyService.findById(companyId);
        return vehicles.stream()
                .filter(vehicle -> vehicle.companyId().equals(companyId))
                .toList();
    }

    public VehicleResponse findById(Long id) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException("Vehiculo no encontrado"));
    }

    public VehicleResponse create(CreateVehicleRequest request) {
        companyService.findById(request.companyId());
        boolean exists = vehicles.stream()
                .anyMatch(vehicle -> vehicle.companyId().equals(request.companyId())
                        && (vehicle.code().equalsIgnoreCase(request.code()) || vehicle.plate().equalsIgnoreCase(request.plate())));

        if (exists) {
            throw new ApiException("El vehiculo ya existe en la empresa");
        }

        VehicleResponse vehicle = new VehicleResponse(
                nextId(),
                request.companyId(),
                request.code(),
                request.plate(),
                request.label(),
                "EN_RANGO",
                request.driver(),
                request.imei(),
                request.model(),
                request.year(),
                request.unitType(),
                request.loadCapacityKg(),
                -12.0464,
                -77.0428,
                "--",
                "Sin datos",
                "Cerrada",
                "Encendido",
                "Sin comunicacion"
        );
        vehicles.add(vehicle);
        saveVehicles();
        return vehicle;
    }


    public VehicleResponse update(Long id, CreateVehicleRequest request) {
        VehicleResponse current = findById(id);
        companyService.findById(request.companyId());
        boolean exists = vehicles.stream()
                .anyMatch(vehicle -> !vehicle.id().equals(id)
                        && vehicle.companyId().equals(request.companyId())
                        && (vehicle.code().equalsIgnoreCase(request.code()) || vehicle.plate().equalsIgnoreCase(request.plate())));

        if (exists) {
            throw new ApiException("El vehiculo ya existe en la empresa");
        }

        VehicleResponse updated = new VehicleResponse(
                current.id(),
                request.companyId(),
                request.code(),
                request.plate(),
                request.label(),
                current.status(),
                request.driver(),
                request.imei(),
                request.model(),
                request.year(),
                request.unitType(),
                request.loadCapacityKg(),
                current.latitude(),
                current.longitude(),
                current.currentTemperature(),
                current.temperatureState(),
                current.doorState(),
                current.coolingUnitState(),
                current.lastCommunication()
        );
        vehicles.set(vehicles.indexOf(current), updated);
        saveVehicles();
        return updated;
    }

    public VehicleResponse updateTelemetryState(
            Long id,
            Double latitude,
            Double longitude,
            String currentTemperature,
            String temperatureState,
            String doorState,
            String coolingUnitState,
            String lastCommunication
    ) {
        VehicleResponse current = findById(id);
        String nextStatus = vehicleStatus(currentTemperature, temperatureState, current.status());
        VehicleResponse updated = new VehicleResponse(
                current.id(),
                current.companyId(),
                current.code(),
                current.plate(),
                current.label(),
                nextStatus,
                current.driver(),
                current.imei(),
                current.model(),
                current.year(),
                current.unitType(),
                current.loadCapacityKg(),
                latitude == null ? current.latitude() : latitude,
                longitude == null ? current.longitude() : longitude,
                currentTemperature == null ? current.currentTemperature() : currentTemperature,
                temperatureState == null ? current.temperatureState() : temperatureState,
                doorState == null ? current.doorState() : doorState,
                coolingUnitState == null ? current.coolingUnitState() : coolingUnitState,
                lastCommunication == null ? current.lastCommunication() : lastCommunication
        );
        vehicles.set(vehicles.indexOf(current), updated);
        saveVehicles();
        return updated;
    }

    private String vehicleStatus(String currentTemperature, String temperatureState, String currentStatus) {
        if (currentTemperature == null || currentTemperature.isBlank()) {
            return currentStatus;
        }
        Double value = parseTemperature(currentTemperature);
        if (value == null) {
            return currentStatus;
        }
        if (value > 8 || value < -5) {
            return "CRITICO";
        }
        if (temperatureState != null && temperatureState.equalsIgnoreCase("Fuera de rango")) {
            return "ADVERTENCIA";
        }
        return "EN_RANGO";
    }

    private Double parseTemperature(String value) {
        try {
            return Double.parseDouble(value.replace("°C", "").replace("C", "").trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Long nextId() {
        return vehicles.stream().mapToLong(VehicleResponse::id).max().orElse(0L) + 1;
    }

    private void loadVehicles() {
        try {
            if (Files.exists(storePath)) {
                vehicles.addAll(objectMapper.readValue(storePath.toFile(), new TypeReference<List<VehicleResponse>>() {}));
                return;
            }

            vehicles.addAll(defaultVehicles());
            saveVehicles();
        } catch (IOException ex) {
            throw new ApiException("No se pudo cargar vehiculos persistidos");
        }
    }

    private void saveVehicles() {
        try {
            Files.createDirectories(storePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), vehicles);
        } catch (IOException ex) {
            throw new ApiException("No se pudo persistir vehiculos");
        }
    }

    private List<VehicleResponse> defaultVehicles() {
        return List.of(
                new VehicleResponse(1L, 1L, "AAA111", "AAA-111", "Camion 01 - AAA111", "EN_RANGO", "Carlos Ruiz", "865612040015601", "Hino 500", 2022, "Refrigerado", 12000, -11.985, -77.065, "2.1 °C", "En rango", "Cerrada", "Encendido", "Hace 1 min"),
                new VehicleResponse(2L, 1L, "BBB222", "BBB-222", "Camion 02 - BBB222", "EN_RANGO", "Luis Mendoza", "865612040015602", "Hino 500", 2021, "Refrigerado", 12000, -12.010, -77.115, "3.4 °C", "En rango", "Cerrada", "Encendido", "Hace 2 min"),
                new VehicleResponse(3L, 1L, "GHI789", "GHI-789", "Camion 03 - GHI789", "ADVERTENCIA", "Marco Salas", "865612040015603", "Isuzu NPR", 2020, "Refrigerado", 9000, -12.027, -77.014, "6.7 °C", "Fuera de rango", "Cerrada", "Encendido", "Hace 2 min"),
                new VehicleResponse(7L, 1L, "DEF456", "DEF-456", "Camion 07 - DEF456", "ADVERTENCIA", "Rafael Torres", "865612040015607", "Hino 500", 2022, "Refrigerado", 12000, -12.071, -76.995, "4.8 °C", "En rango", "Abierta", "Encendido", "Hace 3 min"),
                new VehicleResponse(12L, 1L, "ABC123", "ABC-123", "Camion 12 - ABC123", "CRITICO", "Juan Perez", "865612040015678", "Hino 500", 2022, "Refrigerado", 12000, -12.0576, -76.9649, "9.8 °C", "Fuera de rango", "Cerrada", "Encendido", "Hace 1 min"),
                new VehicleResponse(21L, 1L, "MNO321", "MNO-321", "Camion 21 - MNO321", "SIN_COMUNICACION", "Pedro Vargas", "865612040015621", "Foton Aumark", 2019, "Refrigerado", 8000, -12.115, -77.035, null, "Sin datos", "--", "--", "Hace 25 min"),
                new VehicleResponse(101L, 2L, "NOR111", "NOR-111", "Camion Norte 01 - NOR111", "ADVERTENCIA", "Ana Castro", "865612040015701", "Hino 500", 2022, "Refrigerado", 12000, -8.1116, -79.0288, "6.2 °C", "Fuera de rango", "Cerrada", "Encendido", "Hace 4 min")
        );
    }
}
