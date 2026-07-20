package com.mt.friotrackapi.tracking.service;

import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.telemetry.entity.TelemetryReadingEntity;
import com.mt.friotrackapi.telemetry.service.TelemetryService;
import com.mt.friotrackapi.tracking.dto.TripReadingResponse;
import com.mt.friotrackapi.tracking.dto.TripResponse;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class TrackingService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Duration TRIP_GAP = Duration.ofMinutes(45);

    private final TelemetryService telemetryService;
    private final VehicleService vehicleService;
    private final ProtocolConfigService protocolConfigService;

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

        List<List<TelemetryReadingEntity>> groups = new ArrayList<>();
        List<TelemetryReadingEntity> current = new ArrayList<>();
        for (TelemetryReadingEntity reading : readings) {
            if (!current.isEmpty()) {
                Instant previous = current.get(current.size() - 1).getRecordedAt();
                if (Duration.between(previous, reading.getRecordedAt()).compareTo(TRIP_GAP) > 0) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
            }
            current.add(reading);
        }
        if (!current.isEmpty()) groups.add(current);

        Range range = parseRange(protocolConfigService.targetRangeLabel(vehicle.companyId()));
        List<TripResponse> result = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            result.add(toTrip(vehicle, groups.get(i), i + 1, range));
        }
        java.util.Collections.reverse(result);
        return result;
    }

    public byte[] exportCsv(Long vehicleId, int days) {
        StringBuilder csv = new StringBuilder("viaje,fecha,hora,latitud,longitud,temperatura,humedad,puerta,frio,fuera_rango\n");
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
            responseReadings.add(new TripReadingResponse(time(r.getRecordedAt()), r.getTemperature(), value(r.getHumidity()), value(r.getDoorState()), value(r.getCoolingUnitState()), r.getLatitude(), r.getLongitude(), out));
            previous = r;
        }
        int outCount = tempCount - inRange;
        long minutes = Math.max(1, Duration.between(first.getRecordedAt(), last.getRecordedAt()).toMinutes());
        return new TripResponse(
                vehicle.id() + "-" + first.getRecordedAt().toEpochMilli(),
                "Viaje " + index + " - " + vehicle.label(),
                date(first.getRecordedAt()),
                time(first.getRecordedAt()) + " - " + time(last.getRecordedAt()),
                first.getLatitude(), first.getLongitude(), last.getLatitude(), last.getLongitude(),
                responseReadings,
                String.format(Locale.US, "%.1f", distance),
                tempCount == 0 ? "--" : String.format(Locale.US, "%.1f °C", sum / tempCount),
                tempCount == 0 ? "--" : String.format(Locale.US, "%.1f °C", min),
                tempCount == 0 ? "--" : String.format(Locale.US, "%.1f °C", max),
                percent(inRange, tempCount),
                percent(outCount, tempCount),
                doorOpenings,
                minutes < 60 ? minutes + " min" : (minutes / 60) + " h " + (minutes % 60) + " min"
        );
    }

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
