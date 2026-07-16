package com.mt.friotrackapi.telemetry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.telemetry.dto.CreateVehicleEventRequest;
import com.mt.friotrackapi.telemetry.dto.SaveTemperatureHistoryRequest;
import com.mt.friotrackapi.telemetry.dto.TelemetrySnapshotResponse;
import com.mt.friotrackapi.telemetry.dto.TemperatureChartResponse;
import com.mt.friotrackapi.telemetry.dto.TemperaturePointResponse;
import com.mt.friotrackapi.telemetry.dto.UpdateTelemetrySnapshotRequest;
import com.mt.friotrackapi.telemetry.dto.VehicleDailySummaryResponse;
import com.mt.friotrackapi.telemetry.dto.VehicleEventResponse;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelemetryService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path snapshotsPath = Path.of(System.getProperty("user.dir"), "data", "telemetry-snapshots.json");
    private final Path historyPath = Path.of(System.getProperty("user.dir"), "data", "temperature-history.json");
    private final Path eventsPath = Path.of(System.getProperty("user.dir"), "data", "vehicle-events.json");
    private final Map<Long, TelemetrySnapshotResponse> snapshots = new LinkedHashMap<>();
    private final Map<Long, List<TemperaturePointResponse>> history = new LinkedHashMap<>();
    private final Map<Long, List<VehicleEventResponse>> events = new LinkedHashMap<>();
    private final VehicleService vehicleService;
    private final ProtocolConfigService protocolConfigService;

    public TelemetryService(VehicleService vehicleService, ProtocolConfigService protocolConfigService) {
        this.vehicleService = vehicleService;
        this.protocolConfigService = protocolConfigService;
        loadAll();
    }

    public TelemetrySnapshotResponse snapshot(Long vehicleId) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        TelemetrySnapshotResponse snapshot = rawSnapshot(vehicle);
        return maskSnapshot(vehicle.companyId(), snapshot);
    }

    private TelemetrySnapshotResponse rawSnapshot(VehicleResponse vehicle) {
        TelemetrySnapshotResponse snapshot = snapshots.get(vehicle.id());
        if (snapshot != null) {
            return snapshot;
        }

        return new TelemetrySnapshotResponse(
                vehicle.id(),
                vehicle.currentTemperature() == null ? "--" : vehicle.currentTemperature(),
                vehicle.temperatureState(),
                "--",
                vehicle.doorState(),
                vehicle.coolingUnitState(),
                "--",
                "--",
                "-2 °C a 5 °C",
                vehicle.latitude(),
                vehicle.longitude(),
                "Sin direccion registrada",
                vehicle.lastCommunication()
        );
    }

    public TelemetrySnapshotResponse updateSnapshot(UpdateTelemetrySnapshotRequest request) {
        VehicleResponse vehicle = vehicleService.findById(request.vehicleId());
        TelemetrySnapshotResponse snapshot = new TelemetrySnapshotResponse(
                request.vehicleId(),
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
        snapshots.put(request.vehicleId(), snapshot);
        saveSnapshots();
        return maskSnapshot(vehicle.companyId(), snapshot);
    }

    public List<TemperaturePointResponse> temperatureHistory(Long vehicleId) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        if (!protocolConfigService.isFieldEnabled(vehicle.companyId(), "temperature")) {
            return List.of();
        }
        return history.getOrDefault(vehicleId, defaultHistory());
    }

    public List<TemperaturePointResponse> saveTemperatureHistory(SaveTemperatureHistoryRequest request) {
        vehicleService.findById(request.vehicleId());
        List<TemperaturePointResponse> points = new ArrayList<>(request.points());
        history.put(request.vehicleId(), points);
        saveHistory();
        return points;
    }

    public TemperatureChartResponse temperatureChart(Long vehicleId) {
        TelemetrySnapshotResponse snapshot = snapshot(vehicleId);
        List<TemperaturePointResponse> points = temperatureHistory(vehicleId);
        Range range = parseRange(snapshot.targetRange());
        return new TemperatureChartResponse(
                points,
                range.min(),
                range.max(),
                String.format("Min: %.0f °C", range.min()),
                String.format("Max: %.0f °C", range.max()),
                Math.min(-5.0, range.min() - 3.0),
                Math.max(15.0, range.max() + 10.0),
                5
        );
    }

    public VehicleDailySummaryResponse dailySummary(Long vehicleId) {
        TelemetrySnapshotResponse snapshot = snapshot(vehicleId);
        List<TemperaturePointResponse> points = temperatureHistory(vehicleId);
        List<VehicleEventResponse> vehicleEvents = events(vehicleId);
        Range range = parseRange(snapshot.targetRange());

        int total = points.size();
        int inRange = 0;
        double sum = 0;

        for (TemperaturePointResponse point : points) {
            double temperature = point.temperature();
            if (temperature >= range.min() && temperature <= range.max()) {
                inRange++;
            }
            sum += temperature;
        }

        int outOfRange = total - inRange;
        long doorOpenings = vehicleEvents.stream()
                .filter(event -> "DOOR".equalsIgnoreCase(event.type()))
                .count();
        String average = total == 0 ? "--" : String.format(java.util.Locale.US, "%.1f °C", sum / total);

        return new VehicleDailySummaryResponse(
                percent(inRange, total),
                inRange + "/" + total + " lecturas",
                percent(outOfRange, total),
                outOfRange + "/" + total + " lecturas",
                (int) doorOpenings,
                "Hoy",
                average,
                "Hoy"
        );
    }

    public List<VehicleEventResponse> events(Long vehicleId) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        return events.getOrDefault(vehicleId, defaultEvents()).stream()
                .filter(event -> protocolConfigService.isEventTypeEnabled(vehicle.companyId(), event.type()))
                .toList();
    }

    public VehicleEventResponse addEvent(CreateVehicleEventRequest request) {
        vehicleService.findById(request.vehicleId());
        VehicleEventResponse event = new VehicleEventResponse(
                request.type(),
                request.title(),
                request.description(),
                request.time(),
                request.severity()
        );
        List<VehicleEventResponse> vehicleEvents = new ArrayList<>(events.getOrDefault(request.vehicleId(), List.of()));
        vehicleEvents.add(0, event);
        events.put(request.vehicleId(), vehicleEvents);
        saveEvents();
        return event;
    }

    private TelemetrySnapshotResponse maskSnapshot(Long companyId, TelemetrySnapshotResponse snapshot) {
        boolean temperature = protocolConfigService.isFieldEnabled(companyId, "temperature");
        boolean humidity = protocolConfigService.isFieldEnabled(companyId, "humidity");
        boolean door = protocolConfigService.isFieldEnabled(companyId, "doorState");
        boolean cooling = protocolConfigService.isFieldEnabled(companyId, "coolingUnitState");
        boolean fuel = protocolConfigService.isFieldEnabled(companyId, "fuelLevel");
        boolean speed = protocolConfigService.isFieldEnabled(companyId, "speed");
        boolean latitude = protocolConfigService.isFieldEnabled(companyId, "latitude");
        boolean longitude = protocolConfigService.isFieldEnabled(companyId, "longitude");

        return new TelemetrySnapshotResponse(
                snapshot.vehicleId(),
                temperature ? snapshot.temperature() : null,
                temperature ? snapshot.temperatureState() : null,
                humidity ? snapshot.humidity() : null,
                door ? snapshot.doorState() : null,
                cooling ? snapshot.coolingUnitState() : null,
                fuel ? snapshot.fuelLevel() : null,
                speed ? snapshot.speed() : null,
                temperature ? snapshot.targetRange() : null,
                latitude ? snapshot.latitude() : null,
                longitude ? snapshot.longitude() : null,
                latitude && longitude ? snapshot.address() : null,
                snapshot.lastCommunication()
        );
    }

    private void loadAll() {
        loadSnapshots();
        loadHistory();
        loadEvents();
    }

    private void loadSnapshots() {
        try {
            if (Files.exists(snapshotsPath)) {
                snapshots.putAll(objectMapper.readValue(snapshotsPath.toFile(), new TypeReference<Map<Long, TelemetrySnapshotResponse>>() {}));
                return;
            }
            snapshots.put(12L, defaultSnapshot());
            saveSnapshots();
        } catch (IOException ex) {
            throw new ApiException("No se pudo cargar snapshots de telemetria");
        }
    }

    private void loadHistory() {
        try {
            if (Files.exists(historyPath)) {
                history.putAll(objectMapper.readValue(historyPath.toFile(), new TypeReference<Map<Long, List<TemperaturePointResponse>>>() {}));
                return;
            }
            history.put(12L, defaultHistory());
            saveHistory();
        } catch (IOException ex) {
            throw new ApiException("No se pudo cargar historial de temperatura");
        }
    }

    private void loadEvents() {
        try {
            if (Files.exists(eventsPath)) {
                events.putAll(objectMapper.readValue(eventsPath.toFile(), new TypeReference<Map<Long, List<VehicleEventResponse>>>() {}));
                return;
            }
            events.put(12L, defaultEvents());
            saveEvents();
        } catch (IOException ex) {
            throw new ApiException("No se pudo cargar eventos de vehiculo");
        }
    }

    private void saveSnapshots() {
        write(snapshotsPath, snapshots, "No se pudo persistir snapshots de telemetria");
    }

    private void saveHistory() {
        write(historyPath, history, "No se pudo persistir historial de temperatura");
    }

    private void saveEvents() {
        write(eventsPath, events, "No se pudo persistir eventos de vehiculo");
    }

    private void write(Path path, Object value, String errorMessage) {
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
        } catch (IOException ex) {
            throw new ApiException(errorMessage);
        }
    }


    private static int percent(int value, int total) {
        return total == 0 ? 0 : Math.round((value * 100.0f) / total);
    }

    private static Range parseRange(String targetRange) {
        if (targetRange == null || targetRange.isBlank()) {
            return new Range(-2.0, 5.0);
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(targetRange);
        java.util.List<Double> values = new java.util.ArrayList<>();
        while (matcher.find()) {
            values.add(Double.parseDouble(matcher.group()));
        }

        if (values.size() >= 2) {
            return new Range(values.get(0), values.get(1));
        }

        return new Range(-2.0, 5.0);
    }

    private record Range(double min, double max) {
    }

    private TelemetrySnapshotResponse defaultSnapshot() {
        return new TelemetrySnapshotResponse(
                12L,
                "9.8 °C",
                "Fuera de rango",
                "45 %",
                "Abierta",
                "Encendido",
                "65 %",
                "65 km/h",
                "-2 °C a 5 °C",
                -12.0576,
                -76.9649,
                "Av. Nicolas Ayllon 3980, Ate, Lima",
                "Hace 1 min"
        );
    }

    private List<TemperaturePointResponse> defaultHistory() {
        return List.of(
                new TemperaturePointResponse("12:00", 2.4),
                new TemperaturePointResponse("14:00", 0.3),
                new TemperaturePointResponse("16:00", -1.4),
                new TemperaturePointResponse("18:00", -0.7),
                new TemperaturePointResponse("20:00", -0.8),
                new TemperaturePointResponse("22:00", -1.5),
                new TemperaturePointResponse("00:00", -2.2),
                new TemperaturePointResponse("02:00", -0.4),
                new TemperaturePointResponse("04:00", 0.8),
                new TemperaturePointResponse("06:00", 3.2),
                new TemperaturePointResponse("08:00", 7.4),
                new TemperaturePointResponse("10:00", 10.4),
                new TemperaturePointResponse("12:00", 12.2)
        );
    }

    private List<VehicleEventResponse> defaultEvents() {
        return List.of(
                new VehicleEventResponse("TEMPERATURE", "Temperatura alta: 9.8 °C", "Fuera de rango", "10:32", "CRITICAL"),
                new VehicleEventResponse("DOOR", "Puerta abierta", "Puerta delantera", "10:30", "WARNING"),
                new VehicleEventResponse("COOLING", "Equipo de frio encendido", "Compresor activado", "10:28", "INFO"),
                new VehicleEventResponse("NETWORK", "Comunicacion restablecida", "Senal OK", "10:27", "INFO")
        );
    }
}
