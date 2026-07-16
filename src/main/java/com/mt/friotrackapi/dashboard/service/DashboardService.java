package com.mt.friotrackapi.dashboard.service;

import com.mt.friotrackapi.alerts.dto.AlertResponse;
import com.mt.friotrackapi.alerts.service.AlertService;
import com.mt.friotrackapi.dashboard.dto.DashboardSummaryResponse;
import com.mt.friotrackapi.dashboard.dto.FeaturedVehicleResponse;
import com.mt.friotrackapi.dashboard.dto.FleetMapVehicleResponse;
import com.mt.friotrackapi.dashboard.dto.TemperatureDistributionResponse;
import com.mt.friotrackapi.dashboard.dto.VehicleStatusResponse;
import com.mt.friotrackapi.common.exception.ForbiddenException;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.telemetry.service.TelemetryService;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

@Service
public class DashboardService {

    private static final String TARGET_RANGE = "-2 °C a 5 °C";

    private final VehicleService vehicleService;
    private final AlertService alertService;
    private final TelemetryService telemetryService;
    private final ProtocolConfigService protocolConfigService;

    public DashboardService(VehicleService vehicleService, AlertService alertService, TelemetryService telemetryService, ProtocolConfigService protocolConfigService) {
        this.vehicleService = vehicleService;
        this.alertService = alertService;
        this.telemetryService = telemetryService;
        this.protocolConfigService = protocolConfigService;
    }

    public DashboardSummaryResponse summary(Long companyId) {
        List<VehicleResponse> vehicles = vehicleService.findAll(companyId);
        Counts counts = countStatuses(vehicles);
        boolean temperatureEnabled = protocolConfigService.isFieldEnabled(companyId, "temperature");

        OptionalDouble average = temperatureEnabled
                ? vehicles.stream()
                        .map(VehicleResponse::currentTemperature)
                        .map(DashboardService::parseTemperature)
                        .filter(Double::isFinite)
                        .mapToDouble(Double::doubleValue)
                        .average()
                : OptionalDouble.empty();

        String averageTemperature = average.isPresent()
                ? String.format(Locale.US, "%.1f °C", average.getAsDouble())
                : null;

        int total = vehicles.size();
        return new DashboardSummaryResponse(
                total,
                counts.inRange(),
                percent(counts.inRange(), total),
                counts.warning(),
                percent(counts.warning(), total),
                counts.outOfRange(),
                percent(counts.outOfRange(), total),
                counts.offline(),
                percent(counts.offline(), total),
                averageTemperature,
                average.isPresent() ? averageTemperatureState(average.getAsDouble()) : null
        );
    }

    public List<FleetMapVehicleResponse> fleetMap(Long companyId) {
        boolean latitudeEnabled = protocolConfigService.isFieldEnabled(companyId, "latitude");
        boolean longitudeEnabled = protocolConfigService.isFieldEnabled(companyId, "longitude");
        if (!latitudeEnabled || !longitudeEnabled) {
            return List.of();
        }

        boolean temperatureEnabled = protocolConfigService.isFieldEnabled(companyId, "temperature");
        List<VehicleResponse> vehicles = vehicleService.findAll(companyId);
        MapCenter center = mapCenter(vehicles);
        return vehicles.stream()
                .map(vehicle -> new FleetMapVehicleResponse(
                        vehicle.id(),
                        vehicle.label(),
                        vehicle.latitude(),
                        vehicle.longitude(),
                        center.latitude(),
                        center.longitude(),
                        vehicles.isEmpty() ? 10.0 : 10.8,
                        vehicle.status(),
                        statusLabel(vehicle.status()),
                        statusColor(vehicle.status()),
                        temperatureEnabled ? (vehicle.currentTemperature() == null ? "Sin datos" : vehicle.currentTemperature()) : null
                ))
                .toList();
    }

    public List<VehicleStatusResponse> vehicleStatus(Long companyId) {
        boolean temperatureEnabled = protocolConfigService.isFieldEnabled(companyId, "temperature");
        boolean doorEnabled = protocolConfigService.isFieldEnabled(companyId, "doorState");
        boolean coolingEnabled = protocolConfigService.isFieldEnabled(companyId, "coolingUnitState");

        return vehicleService.findAll(companyId).stream()
                .map(vehicle -> new VehicleStatusResponse(
                        vehicle.id(),
                        vehicle.label(),
                        statusLabel(vehicle.status()),
                        statusColorKey(vehicle.status()),
                        temperatureEnabled ? (vehicle.currentTemperature() == null ? "--" : vehicle.currentTemperature()) : null,
                        temperatureEnabled ? vehicle.temperatureState() : null,
                        temperatureEnabled ? statusColorKey(vehicle.temperatureState()) : null,
                        doorEnabled ? vehicle.doorState() : null,
                        coolingEnabled ? vehicle.coolingUnitState() : null,
                        vehicle.lastCommunication()
                ))
                .toList();
    }

    public TemperatureDistributionResponse temperatureDistribution(Long companyId) {
        if (!protocolConfigService.isFieldEnabled(companyId, "temperature")) {
            return new TemperatureDistributionResponse(0, 0, 0, 0, 0, 0, 0, 0, 0, null);
        }

        Counts counts = countStatuses(vehicleService.findAll(companyId));
        int total = counts.total();
        return new TemperatureDistributionResponse(
                total,
                counts.inRange(),
                percent(counts.inRange(), total),
                counts.warning(),
                percent(counts.warning(), total),
                counts.outOfRange(),
                percent(counts.outOfRange(), total),
                counts.offline(),
                percent(counts.offline(), total),
                TARGET_RANGE
        );
    }

    public List<AlertResponse> recentAlerts(Long companyId, int limit) {
        return alertService.findAll(companyId, null).stream()
                .limit(Math.max(1, limit))
                .toList();
    }

    public FeaturedVehicleResponse featuredVehicle(Long companyId, Long vehicleId) {
        VehicleResponse vehicle = vehicleId == null
                ? vehicleService.findAll(companyId).stream()
                        .filter(item -> "CRITICO".equals(normalizeStatus(item.status())))
                        .findFirst()
                        .orElseGet(() -> vehicleService.findAll(companyId).stream()
                                .findFirst()
                                .orElseThrow(() -> new com.mt.friotrackapi.common.exception.ApiException("No hay vehiculos para la empresa")))
                : vehicleService.findById(vehicleId);

        if (!vehicle.companyId().equals(companyId)) {
            throw new ForbiddenException("No tienes acceso a este vehiculo");
        }

        return new FeaturedVehicleResponse(vehicle, telemetryService.snapshot(vehicle.id()));
    }

    private Counts countStatuses(List<VehicleResponse> vehicles) {
        int inRange = 0;
        int warning = 0;
        int outOfRange = 0;
        int offline = 0;

        for (VehicleResponse vehicle : vehicles) {
            String status = normalizeStatus(vehicle.status());
            if ("EN_RANGO".equals(status)) {
                inRange++;
            } else if ("ADVERTENCIA".equals(status)) {
                warning++;
            } else if ("CRITICO".equals(status)) {
                outOfRange++;
            } else if ("SIN_COMUNICACION".equals(status) || "SIN_DATOS".equals(status)) {
                offline++;
            }
        }

        return new Counts(inRange, warning, outOfRange, offline);
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return "SIN_DATOS";
        }
        return status.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String statusColor(String status) {
        return switch (normalizeStatus(status)) {
            case "EN_RANGO" -> "#35b553";
            case "ADVERTENCIA" -> "#ffad0a";
            case "CRITICO" -> "#ff2d2d";
            default -> "#70809b";
        };
    }

    private static String statusLabel(String status) {
        return switch (normalizeStatus(status)) {
            case "EN_RANGO" -> "En rango";
            case "ADVERTENCIA" -> "Advertencia";
            case "CRITICO" -> "Critico";
            case "FUERA_DE_RANGO" -> "Fuera de rango";
            case "SIN_COMUNICACION", "SIN_DATOS" -> "Sin comunicacion";
            default -> status == null ? "Sin datos" : status;
        };
    }

    private static String statusColorKey(String status) {
        return switch (normalizeStatus(status)) {
            case "EN_RANGO" -> "green";
            case "ADVERTENCIA" -> "orange";
            case "CRITICO", "FUERA_DE_RANGO" -> "red";
            default -> "gray";
        };
    }

    private static int percent(int value, int total) {
        return total == 0 ? 0 : Math.round((value * 100.0f) / total);
    }

    private static String averageTemperatureState(double average) {
        if (average < -2 || average > 5) {
            return "fuera de rango";
        }
        return "en rango";
    }

    private static MapCenter mapCenter(List<VehicleResponse> vehicles) {
        if (vehicles.isEmpty()) {
            return new MapCenter(-12.0464, -77.0428);
        }

        double latitude = vehicles.stream().mapToDouble(VehicleResponse::latitude).average().orElse(-12.0464);
        double longitude = vehicles.stream().mapToDouble(VehicleResponse::longitude).average().orElse(-77.0428);
        return new MapCenter(latitude, longitude);
    }

    private static Double parseTemperature(String temperature) {
        if (temperature == null || temperature.isBlank()) {
            return Double.NaN;
        }

        try {
            return Double.parseDouble(temperature.replace("°C", "").replace("C", "").trim());
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private record Counts(int inRange, int warning, int outOfRange, int offline) {
        private int total() {
            return inRange + warning + outOfRange + offline;
        }
    }

    private record MapCenter(double latitude, double longitude) {
    }
}
