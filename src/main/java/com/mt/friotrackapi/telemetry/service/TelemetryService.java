package com.mt.friotrackapi.telemetry.service;

import com.mt.friotrackapi.common.dto.PageResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.mqtt.dto.ProtocolTelemetryData;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.realtime.service.RealtimeEventService;
import com.mt.friotrackapi.telemetry.dto.CreateVehicleEventRequest;
import com.mt.friotrackapi.telemetry.dto.SaveTemperatureHistoryRequest;
import com.mt.friotrackapi.telemetry.dto.TelemetrySnapshotResponse;
import com.mt.friotrackapi.telemetry.dto.TelemetryReadingResponse;
import com.mt.friotrackapi.telemetry.dto.TemperatureChartResponse;
import com.mt.friotrackapi.telemetry.dto.TemperaturePointResponse;
import com.mt.friotrackapi.telemetry.dto.UpdateTelemetrySnapshotRequest;
import com.mt.friotrackapi.telemetry.dto.VehicleDailySummaryResponse;
import com.mt.friotrackapi.telemetry.dto.VehicleEventResponse;
import com.mt.friotrackapi.telemetry.entity.TelemetryReadingEntity;
import com.mt.friotrackapi.telemetry.entity.TelemetrySnapshotEntity;
import com.mt.friotrackapi.telemetry.entity.VehicleEventEntity;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TelemetryService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final VehicleService vehicleService;
    private final ProtocolConfigService protocolConfigService;
    private final RealtimeEventService realtimeEventService;

    @PersistenceContext
    private EntityManager entityManager;

    public TelemetryService(VehicleService vehicleService, ProtocolConfigService protocolConfigService, RealtimeEventService realtimeEventService) {
        this.vehicleService = vehicleService;
        this.protocolConfigService = protocolConfigService;
        this.realtimeEventService = realtimeEventService;
    }

    public TelemetrySnapshotResponse snapshot(Long vehicleId) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        return maskSnapshot(vehicle, rawSnapshot(vehicle));
    }

    @Transactional
    public TelemetrySnapshotResponse applyMqttTelemetry(VehicleResponse vehicle, ProtocolTelemetryData data) {
        return applyMqttTelemetry(vehicle, data, null);
    }

    @Transactional
    public TelemetrySnapshotResponse applyMqttTelemetry(VehicleResponse vehicle, ProtocolTelemetryData data, String rawPayload) {
        TelemetrySnapshotResponse current = rawSnapshot(vehicle);
        Map<String, Object> customFields = new LinkedHashMap<>(current.customFields() == null ? Map.of() : current.customFields());
        if (data.customFields() != null) customFields.putAll(data.customFields());

        String targetRange = protocolConfigService.targetRangeLabel(vehicle.companyId(), vehicle.detectedProtocol());
        TelemetrySnapshotResponse snapshot = new TelemetrySnapshotResponse(
                vehicle.id(),
                data.temperature() == null ? current.temperature() : data.temperature(),
                data.temperatureState() == null ? current.temperatureState() : data.temperatureState(),
                data.humidity() == null ? current.humidity() : data.humidity(),
                data.doorState() == null ? current.doorState() : data.doorState(),
                data.coolingUnitState() == null ? current.coolingUnitState() : data.coolingUnitState(),
                data.fuelLevel() == null ? current.fuelLevel() : data.fuelLevel(),
                data.speed() == null ? current.speed() : data.speed(),
                targetRange,
                data.latitude() == null ? current.latitude() : data.latitude(),
                data.longitude() == null ? current.longitude() : data.longitude(),
                data.latitude() != null && data.longitude() != null ? "Ubicacion MQTT" : current.address(),
                "Ahora",
                customFields
        );

        saveSnapshot(vehicle.id(), snapshot);
        persistReading(vehicle, data, customFields, rawPayload);

        TelemetrySnapshotResponse response = maskSnapshot(vehicle, snapshot);
        realtimeEventService.publish(vehicle.companyId(), "telemetry", Map.of("vehicleId", vehicle.id(), "snapshot", response));
        return response;
    }

    @Transactional
    public void recordMqttEvent(Long vehicleId, String type, String title, String description, String severity) {
        VehicleEntity vehicle = vehicleEntity(vehicleId);
        List<VehicleEventEntity> latest = entityManager.createQuery("select e from VehicleEventEntity e where e.vehicle.id = :vehicleId order by e.occurredAt desc", VehicleEventEntity.class)
                .setParameter("vehicleId", vehicleId)
                .setMaxResults(1)
                .getResultList();
        VehicleEventResponse next = new VehicleEventResponse(type, title, description, timeLabel(Instant.now()), severity);
        if (!latest.isEmpty() && sameEvent(toEventResponse(latest.get(0)), next)) return;
        entityManager.persist(new VehicleEventEntity(vehicle, type, title, description, Instant.now(), severity));
        realtimeEventService.publish(vehicle.getCompany().getId(), "telemetry", Map.of("vehicleId", vehicleId, "event", next));
    }

    @Transactional
    public TelemetrySnapshotResponse updateSnapshot(UpdateTelemetrySnapshotRequest request) {
        VehicleResponse vehicle = vehicleService.findById(request.vehicleId());
        TelemetrySnapshotResponse current = rawSnapshot(vehicle);
        TelemetrySnapshotResponse snapshot = new TelemetrySnapshotResponse(
                request.vehicleId(), request.temperature(), request.temperatureState(), request.humidity(), request.doorState(),
                request.coolingUnitState(), request.fuelLevel(), request.speed(), request.targetRange(), request.latitude(), request.longitude(),
                request.address(), request.lastCommunication(), current.customFields() == null ? Map.of() : current.customFields()
        );
        saveSnapshot(request.vehicleId(), snapshot);
        TelemetrySnapshotResponse response = maskSnapshot(vehicle, snapshot);
        realtimeEventService.publish(vehicle.companyId(), "telemetry", Map.of("vehicleId", request.vehicleId(), "snapshot", response));
        return response;
    }

    public List<TemperaturePointResponse> temperatureHistory(Long vehicleId) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        if (!protocolConfigService.isFieldEnabled(vehicle.companyId(), vehicle.detectedProtocol(), "temperature")) return List.of();
        return entityManager.createQuery("select r from TelemetryReadingEntity r where r.vehicle.id = :vehicleId and r.temperature is not null order by r.recordedAt asc", TelemetryReadingEntity.class)
                .setParameter("vehicleId", vehicleId)
                .setMaxResults(48)
                .getResultList().stream()
                .map(r -> new TemperaturePointResponse(timeLabel(r.getRecordedAt()), r.getTemperature()))
                .toList();
    }

    @Transactional
    public List<TemperaturePointResponse> saveTemperatureHistory(SaveTemperatureHistoryRequest request) {
        VehicleEntity vehicle = vehicleEntity(request.vehicleId());
        Instant now = Instant.now();
        int index = 0;
        for (TemperaturePointResponse point : request.points()) {
            entityManager.persist(new TelemetryReadingEntity(vehicle, vehicle.getCompany().getId(), now.plusSeconds(index++), point.temperature(), null, null, null, null, null, vehicle.getLatitude(), vehicle.getLongitude(), "{}", null));
        }
        realtimeEventService.publish(vehicle.getCompany().getId(), "telemetry", Map.of("vehicleId", request.vehicleId(), "temperatureHistory", true));
        return temperatureHistory(request.vehicleId());
    }

    public TemperatureChartResponse temperatureChart(Long vehicleId) {
        TelemetrySnapshotResponse snapshot = snapshot(vehicleId);
        List<TemperaturePointResponse> points = temperatureHistory(vehicleId);
        Range range = parseRange(snapshot.targetRange());
        return new TemperatureChartResponse(points, range.min(), range.max(), String.format("Min: %.0f °C", range.min()), String.format("Max: %.0f °C", range.max()), Math.min(-5.0, range.min() - 3.0), Math.max(15.0, range.max() + 10.0), 5);
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
            double t = point.temperature();
            if (t >= range.min() && t <= range.max()) inRange++;
            sum += t;
        }
        int outOfRange = total - inRange;
        long doorOpenings = vehicleEvents.stream().filter(event -> "DOOR".equalsIgnoreCase(event.type())).count();
        String average = total == 0 ? "--" : String.format(Locale.US, "%.1f °C", sum / total);
        return new VehicleDailySummaryResponse(percent(inRange, total), inRange + "/" + total + " lecturas", percent(outOfRange, total), outOfRange + "/" + total + " lecturas", (int) doorOpenings, "Hoy", average, "Hoy");
    }

    public List<VehicleEventResponse> events(Long vehicleId) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        return entityManager.createQuery("select e from VehicleEventEntity e where e.vehicle.id = :vehicleId order by e.occurredAt desc", VehicleEventEntity.class)
                .setParameter("vehicleId", vehicleId)
                .setMaxResults(50)
                .getResultList().stream()
                .map(this::toEventResponse)
                .filter(event -> protocolConfigService.isEventTypeEnabled(vehicle.companyId(), vehicle.detectedProtocol(), event.type()))
                .toList();
    }


    public PageResponse<VehicleEventResponse> eventPage(Long vehicleId, int requestedPage, int requestedSize) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(requestedSize, 200));
        long total = entityManager.createQuery("select count(e) from VehicleEventEntity e where e.vehicle.id = :vehicleId", Long.class)
                .setParameter("vehicleId", vehicleId).getSingleResult();
        List<VehicleEventResponse> items = entityManager.createQuery(
                        "select e from VehicleEventEntity e where e.vehicle.id = :vehicleId order by e.occurredAt desc",
                        VehicleEventEntity.class)
                .setParameter("vehicleId", vehicleId).setFirstResult(page * size).setMaxResults(size)
                .getResultList().stream().map(this::toEventResponse)
                .filter(event -> protocolConfigService.isEventTypeEnabled(vehicle.companyId(), vehicle.detectedProtocol(), event.type()))
                .toList();
        return PageResponse.fromPage(items, page, size, total);
    }

    @Transactional
    public VehicleEventResponse addEvent(CreateVehicleEventRequest request) {
        VehicleEntity vehicle = vehicleEntity(request.vehicleId());
        Instant at = parseEventTime(request.time());
        VehicleEventEntity entity = new VehicleEventEntity(vehicle, request.type(), request.title(), request.description(), at, request.severity());
        entityManager.persist(entity);
        VehicleEventResponse response = toEventResponse(entity);
        realtimeEventService.publish(vehicle.getCompany().getId(), "telemetry", Map.of("vehicleId", request.vehicleId(), "event", response));
        return response;
    }


    public PageResponse<TelemetryReadingResponse> readingPage(Long vehicleId, int days, int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(requestedSize, 200));
        Instant from = Instant.now().minusSeconds(Math.max(1, Math.min(days, 365)) * 86400L);
        long total = entityManager.createQuery(
                        "select count(r) from TelemetryReadingEntity r where r.vehicle.id = :vehicleId and r.recordedAt >= :from",
                        Long.class)
                .setParameter("vehicleId", vehicleId).setParameter("from", from).getSingleResult();
        List<TelemetryReadingResponse> items = entityManager.createQuery(
                        "select r from TelemetryReadingEntity r where r.vehicle.id = :vehicleId and r.recordedAt >= :from order by r.recordedAt desc",
                        TelemetryReadingEntity.class)
                .setParameter("vehicleId", vehicleId).setParameter("from", from)
                .setFirstResult(page * size).setMaxResults(size).getResultList().stream()
                .map(r -> new TelemetryReadingResponse(
                        r.getId(), r.getRecordedAt(), r.getLatitude(), r.getLongitude(), r.getTemperature(),
                        r.getHumidity(), r.getDoorState(), r.getCoolingUnitState(), r.getSpeed(), r.getFuelLevel(),
                        r.getCustomFields()))
                .toList();
        return PageResponse.fromPage(items, page, size, total);
    }

    public List<TelemetryReadingEntity> readings(Long vehicleId, int days) {
        Instant from = Instant.now().minusSeconds(Math.max(1, days) * 86400L);
        return entityManager.createQuery("select r from TelemetryReadingEntity r where r.vehicle.id = :vehicleId and r.recordedAt >= :from order by r.recordedAt asc", TelemetryReadingEntity.class)
                .setParameter("vehicleId", vehicleId)
                .setParameter("from", from)
                .getResultList();
    }

    private TelemetrySnapshotResponse rawSnapshot(VehicleResponse vehicle) {
        TelemetrySnapshotEntity entity = entityManager.find(TelemetrySnapshotEntity.class, vehicle.id());
        if (entity == null) {
            return new TelemetrySnapshotResponse(vehicle.id(), vehicle.currentTemperature() == null ? "--" : vehicle.currentTemperature(), vehicle.temperatureState(), "--", vehicle.doorState(), vehicle.coolingUnitState(), "--", "--", protocolConfigService.targetRangeLabel(vehicle.companyId(), vehicle.detectedProtocol()), vehicle.latitude(), vehicle.longitude(), "Sin direccion registrada", vehicle.lastCommunication(), Map.of());
        }
        return new TelemetrySnapshotResponse(vehicle.id(), entity.getTemperature(), entity.getTemperatureState(), entity.getHumidity(), entity.getDoorState(), entity.getCoolingUnitState(), entity.getFuelLevel(), entity.getSpeed(), entity.getTargetRange(), entity.getLatitude(), entity.getLongitude(), entity.getAddress(), entity.getLastCommunication(), readMap(entity.getCustomFields()));
    }

    private void saveSnapshot(Long vehicleId, TelemetrySnapshotResponse snapshot) {
        VehicleEntity vehicle = vehicleEntity(vehicleId);
        TelemetrySnapshotEntity entity = entityManager.find(TelemetrySnapshotEntity.class, vehicleId);
        if (entity == null) {
            entity = new TelemetrySnapshotEntity(vehicle);
            entityManager.persist(entity);
        }
        entity.update(snapshot.temperature(), snapshot.temperatureState(), snapshot.humidity(), snapshot.doorState(), snapshot.coolingUnitState(), snapshot.fuelLevel(), snapshot.speed(), snapshot.targetRange(), snapshot.latitude(), snapshot.longitude(), snapshot.address(), snapshot.lastCommunication(), writeMap(snapshot.customFields()));
    }

    private void persistReading(VehicleResponse vehicle, ProtocolTelemetryData data, Map<String, Object> customFields, String rawPayload) {
        VehicleEntity entity = vehicleEntity(vehicle.id());
        entityManager.persist(new TelemetryReadingEntity(entity, vehicle.companyId(), Instant.now(), data.temperatureValue(), data.humidity(), data.doorState(), data.coolingUnitState(), data.fuelLevel(), data.speed(), data.latitude(), data.longitude(), writeMap(customFields), rawPayload));
    }

    private TelemetrySnapshotResponse maskSnapshot(VehicleResponse vehicle, TelemetrySnapshotResponse snapshot) {
        Long companyId = vehicle.companyId();
        String protocol = vehicle.detectedProtocol();
        boolean temperature = protocolConfigService.isFieldEnabled(companyId, protocol, "temperature");
        boolean humidity = protocolConfigService.isFieldEnabled(companyId, protocol, "humidity");
        boolean door = protocolConfigService.isFieldEnabled(companyId, protocol, "doorState");
        boolean cooling = protocolConfigService.isFieldEnabled(companyId, protocol, "coolingUnitState");
        boolean fuel = protocolConfigService.isFieldEnabled(companyId, protocol, "fuelLevel");
        boolean speed = protocolConfigService.isFieldEnabled(companyId, protocol, "speed");
        boolean latitude = protocolConfigService.isFieldEnabled(companyId, protocol, "latitude");
        boolean longitude = protocolConfigService.isFieldEnabled(companyId, protocol, "longitude");
        return new TelemetrySnapshotResponse(snapshot.vehicleId(), temperature ? snapshot.temperature() : null, temperature ? snapshot.temperatureState() : null, humidity ? snapshot.humidity() : null, door ? snapshot.doorState() : null, cooling ? snapshot.coolingUnitState() : null, fuel ? snapshot.fuelLevel() : null, speed ? snapshot.speed() : null, temperature ? snapshot.targetRange() : null, latitude ? snapshot.latitude() : null, longitude ? snapshot.longitude() : null, latitude && longitude ? snapshot.address() : null, snapshot.lastCommunication(), maskCustomFields(companyId, protocol, snapshot.customFields()));
    }

    private Map<String, Object> maskCustomFields(Long companyId, String protocol, Map<String, Object> customFields) {
        if (customFields == null || customFields.isEmpty()) return Map.of();
        Map<String, Object> visible = new LinkedHashMap<>();
        customFields.forEach((key, value) -> { if (protocolConfigService.isFieldEnabled(companyId, protocol, key)) visible.put(key, value); });
        return visible;
    }

    private VehicleEntity vehicleEntity(Long vehicleId) {
        VehicleEntity vehicle = entityManager.find(VehicleEntity.class, vehicleId);
        if (vehicle == null) throw new com.mt.friotrackapi.common.exception.ApiException("Vehiculo no encontrado");
        return vehicle;
    }

    private VehicleEventResponse toEventResponse(VehicleEventEntity entity) { return new VehicleEventResponse(entity.getType(), entity.getTitle(), entity.getDescription(), timeLabel(entity.getOccurredAt()), entity.getSeverity()); }
    private boolean sameEvent(VehicleEventResponse a, VehicleEventResponse b) { return eq(a.type(), b.type()) && eq(a.title(), b.title()) && eq(a.description(), b.description()) && eq(a.severity(), b.severity()); }
    private boolean eq(String a, String b) { return a == null ? b == null : a.equalsIgnoreCase(b == null ? "" : b); }
    private String timeLabel(Instant value) { return TIME_FORMAT.format(LocalDateTime.ofInstant(value, LIMA)); }
    private Instant parseEventTime(String value) { return Instant.now(); }
    private Map<String, Object> readMap(String json) { try { return json == null || json.isBlank() ? Map.of() : objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}); } catch (Exception ex) { return Map.of(); } }
    private String writeMap(Map<String, Object> map) { try { return objectMapper.writeValueAsString(map == null ? Map.of() : map); } catch (Exception ex) { return "{}"; } }
    private static int percent(int value, int total) { return total == 0 ? 0 : Math.round((value * 100.0f) / total); }
    private static Range parseRange(String targetRange) {
        if (targetRange == null || targetRange.isBlank()) return new Range(-2.0, 5.0);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(targetRange);
        java.util.List<Double> values = new java.util.ArrayList<>();
        while (matcher.find()) values.add(Double.parseDouble(matcher.group()));
        return values.size() >= 2 ? new Range(values.get(0), values.get(1)) : new Range(-2.0, 5.0);
    }
    private record Range(double min, double max) {}
}
