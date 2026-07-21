package com.mt.friotrackapi.tracking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.telemetry.entity.TelemetryReadingEntity;
import com.mt.friotrackapi.telemetry.entity.VehicleEventEntity;
import com.mt.friotrackapi.telemetry.service.TelemetryService;
import com.mt.friotrackapi.tracking.dto.TripEventResponse;
import com.mt.friotrackapi.tracking.dto.TripReadingResponse;
import com.mt.friotrackapi.tracking.dto.TripResponse;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TrackingService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Duration DATA_GAP = Duration.ofMinutes(45);
    private static final Duration STOP_END = Duration.ofMinutes(30);
    private static final Duration ACTIVE_WINDOW = Duration.ofMinutes(15);
    private static final double MOVING_SPEED_KMH = 3.0;
    private static final double MIN_MOVEMENT_KM = 0.03;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TelemetryService telemetryService;
    private final VehicleService vehicleService;
    private final ProtocolConfigService protocolConfigService;

    @PersistenceContext
    private EntityManager entityManager;

    public TrackingService(TelemetryService telemetryService, VehicleService vehicleService, ProtocolConfigService protocolConfigService) {
        this.telemetryService = telemetryService;
        this.vehicleService = vehicleService;
        this.protocolConfigService = protocolConfigService;
    }

    public List<TripResponse> trips(Long vehicleId, int days) {
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        List<TelemetryReadingEntity> readings = telemetryService.readings(vehicleId, days).stream()
                .filter(r -> r.getLatitude() != null && r.getLongitude() != null)
                .toList();
        if (readings.isEmpty()) return List.of();

        List<List<TelemetryReadingEntity>> groups = segmentTrips(readings);

        Range range = parseRange(protocolConfigService.targetRangeLabel(vehicle.companyId()));
        List<TripResponse> result = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            result.add(toTrip(vehicle, groups.get(i), i + 1, range));
        }
        java.util.Collections.reverse(result);
        return result;
    }

    public byte[] exportCsv(Long vehicleId, int days) {
        StringBuilder csv = new StringBuilder("viaje,fecha,hora,latitud,longitud,temperatura,humedad,puerta,frio,velocidad,combustible,encendido,fuera_rango\n");
        for (TripResponse trip : trips(vehicleId, days)) {
            for (TripReadingResponse reading : trip.readings()) {
                csv.append(escape(trip.name())).append(',')
                        .append(escape(trip.dateLabel())).append(',')
                        .append(escape(reading.time())).append(',')
                        .append(reading.latitude()).append(',')
                        .append(reading.longitude()).append(',')
                        .append(reading.temperature() == null ? "" : reading.temperature()).append(',')
                        .append(escape(reading.humidity())).append(',')
                        .append(escape(reading.doorState())).append(',')
                        .append(escape(reading.coolingUnitState())).append(',')
                        .append(escape(reading.speed())).append(',')
                        .append(escape(reading.fuelLevel())).append(',')
                        .append(reading.ignitionOn() == null ? "" : reading.ignitionOn()).append(',')
                        .append(reading.outOfRange()).append('\n');
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private TripResponse toTrip(VehicleResponse vehicle, List<TelemetryReadingEntity> readings, int index, Range range) {
        TelemetryReadingEntity first = readings.get(0);
        TelemetryReadingEntity last = readings.get(readings.size() - 1);
        double distance = 0;
        double sum = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int tempCount = 0;
        int inRange = 0;
        int doorOpenings = 0;
        List<TripReadingResponse> responseReadings = new ArrayList<>();
        TelemetryReadingEntity previous = null;
        for (TelemetryReadingEntity r : readings) {
            if (previous != null) distance += haversine(previous.getLatitude(), previous.getLongitude(), r.getLatitude(), r.getLongitude());
            if (r.getTemperature() != null) {
                tempCount++;
                sum += r.getTemperature();
                min = Math.min(min, r.getTemperature());
                max = Math.max(max, r.getTemperature());
                if (r.getTemperature() >= range.min() && r.getTemperature() <= range.max()) inRange++;
            }
            if (r.getDoorState() != null && r.getDoorState().toLowerCase(Locale.ROOT).contains("abierta")) doorOpenings++;
            boolean out = r.getTemperature() != null && (r.getTemperature() < range.min() || r.getTemperature() > range.max());
            Map<String, Object> sensors = sensorValues(r);
            responseReadings.add(new TripReadingResponse(
                    r.getRecordedAt().toString(), time(r.getRecordedAt()), r.getTemperature(),
                    value(r.getHumidity()), value(r.getDoorState()), value(r.getCoolingUnitState()),
                    value(r.getSpeed()), value(r.getFuelLevel()), ignitionOn(sensors),
                    r.getLatitude(), r.getLongitude(), out, sensors
            ));
            previous = r;
        }
        int outCount = tempCount - inRange;
        long minutes = Math.max(1, Duration.between(first.getRecordedAt(), last.getRecordedAt()).toMinutes());
        return new TripResponse(
                vehicle.id() + "-" + first.getRecordedAt().toEpochMilli(),
                "Viaje " + index + " - " + vehicle.label(),
                date(first.getRecordedAt()),
                time(first.getRecordedAt()) + " - " + time(last.getRecordedAt()),
                first.getRecordedAt().toString(), last.getRecordedAt().toString(), tripStatus(last),
                first.getLatitude(), first.getLongitude(), last.getLatitude(), last.getLongitude(),
                responseReadings,
                String.format(Locale.US, "%.1f", distance),
                tempCount == 0 ? "--" : String.format(Locale.US, "%.1f °C", sum / tempCount),
                tempCount == 0 ? "--" : String.format(Locale.US, "%.1f °C", min),
                tempCount == 0 ? "--" : String.format(Locale.US, "%.1f °C", max),
                percent(inRange, tempCount),
                percent(outCount, tempCount),
                doorOpenings,
                minutes < 60 ? minutes + " min" : (minutes / 60) + " h " + (minutes % 60) + " min",
                events(vehicle.id(), first.getRecordedAt(), last.getRecordedAt())
        );
    }

    private List<List<TelemetryReadingEntity>> segmentTrips(List<TelemetryReadingEntity> readings) {
        List<List<TelemetryReadingEntity>> groups = new ArrayList<>();
        List<TelemetryReadingEntity> current = new ArrayList<>();
        TelemetryReadingEntity previous = null;
        TelemetryReadingEntity pendingStart = null;
        Instant lastMovementAt = null;

        for (TelemetryReadingEntity reading : readings) {
            boolean dayChanged = previous != null && !localDate(previous.getRecordedAt()).equals(localDate(reading.getRecordedAt()));
            boolean gap = previous != null && Duration.between(previous.getRecordedAt(), reading.getRecordedAt()).compareTo(DATA_GAP) > 0;
            Boolean ignition = ignitionOn(sensorValues(reading));
            boolean moving = isMoving(previous, reading);

            if (!current.isEmpty() && (dayChanged || gap || Boolean.FALSE.equals(ignition))) {
                if (Boolean.FALSE.equals(ignition) && !dayChanged && !gap) current.add(reading);
                groups.add(current);
                current = new ArrayList<>();
                lastMovementAt = null;
            }

            if (current.isEmpty()) {
                if (!Boolean.FALSE.equals(ignition) && (moving || Boolean.TRUE.equals(ignition))) {
                    if (pendingStart != null && !dayChanged && !gap) current.add(pendingStart);
                    current.add(reading);
                    if (moving) lastMovementAt = reading.getRecordedAt();
                }
                pendingStart = reading;
                previous = reading;
                continue;
            }

            if (!current.contains(reading)) current.add(reading);
            if (moving) {
                lastMovementAt = reading.getRecordedAt();
            } else if (lastMovementAt != null && Duration.between(lastMovementAt, reading.getRecordedAt()).compareTo(STOP_END) > 0) {
                groups.add(current);
                current = new ArrayList<>();
                lastMovementAt = null;
            }
            pendingStart = reading;
            previous = reading;
        }
        if (!current.isEmpty()) groups.add(current);
        return groups;
    }

    private boolean isMoving(TelemetryReadingEntity previous, TelemetryReadingEntity current) {
        Double speed = number(current.getSpeed());
        if (speed != null && speed >= MOVING_SPEED_KMH) return true;
        if (previous == null || previous.getLatitude() == null || previous.getLongitude() == null) return false;
        return haversine(previous.getLatitude(), previous.getLongitude(), current.getLatitude(), current.getLongitude()) >= MIN_MOVEMENT_KM;
    }

    private String tripStatus(TelemetryReadingEntity last) {
        Boolean ignition = ignitionOn(sensorValues(last));
        boolean recent = Duration.between(last.getRecordedAt(), Instant.now()).compareTo(ACTIVE_WINDOW) <= 0;
        return recent && !Boolean.FALSE.equals(ignition) ? "IN_PROGRESS" : "COMPLETED";
    }

    private List<TripEventResponse> events(Long vehicleId, Instant from, Instant to) {
        return entityManager.createQuery("select e from VehicleEventEntity e where e.vehicle.id = :vehicleId and e.occurredAt between :from and :to order by e.occurredAt asc", VehicleEventEntity.class)
                .setParameter("vehicleId", vehicleId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList().stream()
                .map(event -> new TripEventResponse(time(event.getOccurredAt()), event.getType(), event.getTitle(), event.getDescription(), event.getSeverity()))
                .toList();
    }

    private Map<String, Object> sensorValues(TelemetryReadingEntity reading) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (reading.getCustomFields() != null && !reading.getCustomFields().isBlank()) {
            try {
                values.putAll(objectMapper.readValue(reading.getCustomFields(), new TypeReference<Map<String, Object>>() {}));
            } catch (Exception ignored) {
                // A malformed optional custom payload must not hide the trip.
            }
        }
        return values;
    }

    private Boolean ignitionOn(Map<String, Object> values) {
        for (String key : List.of("ignitionState", "ignition", "engineOn", "vehicleOn", "encendido")) {
            Object value = values.entrySet().stream().filter(entry -> key.equalsIgnoreCase(entry.getKey())).map(Map.Entry::getValue).findFirst().orElse(null);
            if (value != null) return booleanValue(value);
        }
        return null;
    }

    private Boolean booleanValue(Object value) {
        String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("on") || normalized.equals("encendido");
    }

    private Double number(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.replace("km/h", "").trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDate localDate(Instant instant) { return LocalDateTime.ofInstant(instant, LIMA).toLocalDate(); }
    private String value(String value) { return value == null || value.isBlank() ? "--" : value; }
    private String date(Instant instant) { return DATE.format(LocalDateTime.ofInstant(instant, LIMA)); }
    private String time(Instant instant) { return TIME.format(LocalDateTime.ofInstant(instant, LIMA)); }
    private int percent(int value, int total) { return total == 0 ? 0 : Math.round((value * 100.0f) / total); }
    private String escape(String value) { return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\""; }
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
    private Range parseRange(String targetRange) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(targetRange == null ? "" : targetRange);
        java.util.List<Double> values = new java.util.ArrayList<>();
        while (matcher.find()) values.add(Double.parseDouble(matcher.group()));
        return values.size() >= 2 ? new Range(values.get(0), values.get(1)) : new Range(-2.0, 5.0);
    }
    private record Range(double min, double max) {}
}
