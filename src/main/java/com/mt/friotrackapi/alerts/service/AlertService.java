package com.mt.friotrackapi.alerts.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.alerts.dto.AlertResponse;
import com.mt.friotrackapi.alerts.dto.AlertSummaryResponse;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.persistence.service.JsonStoreService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    private final Path storePath = Path.of(System.getProperty("user.dir"), "data", "alerts.json");
    private final JsonStoreService jsonStoreService;
    private final List<AlertResponse> alerts = new ArrayList<>();
    private final ProtocolConfigService protocolConfigService;

    public AlertService(ProtocolConfigService protocolConfigService, JsonStoreService jsonStoreService) {
        this.protocolConfigService = protocolConfigService;
        this.jsonStoreService = jsonStoreService;
        loadAlerts();
    }

    public List<AlertResponse> findAll(Long companyId, String severity) {
        return findAll(companyId, severity, null, null, null, null);
    }

    public List<AlertResponse> findAll(Long companyId, String severity, String status, String type, String vehicle, String search) {
        String term = normalize(search);
        return alerts.stream()
                .filter(alert -> companyId == null || alert.companyId().equals(companyId))
                .filter(this::isProtocolEnabled)
                .filter(alert -> matches(severity, alert.severity()))
                .filter(alert -> matches(status, alert.status()))
                .filter(alert -> matches(type, alert.type()))
                .filter(alert -> isBlank(vehicle) || contains(alert.vehicleLabel(), vehicle) || contains(alert.vehicleCode(), vehicle))
                .filter(alert -> isBlank(term) || contains(searchText(alert), term))
                .map(this::withCurrentProtocolIcon)
                .toList();
    }

    public AlertResponse findById(Long id) {
        AlertResponse alert = findRawById(id);
        if (!isProtocolEnabled(alert)) {
            throw new ApiException("Alerta no encontrada");
        }
        return withCurrentProtocolIcon(alert);
    }

    private AlertResponse findRawById(Long id) {
        return alerts.stream()
                .filter(alert -> alert.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException("Alerta no encontrada"));
    }

    public AlertSummaryResponse summary(Long companyId) {
        List<AlertResponse> scopedAlerts = findAll(companyId, null).stream()
                .filter(alert -> !alert.status().equalsIgnoreCase("Resuelta"))
                .toList();
        int critical = 0;
        int warning = 0;
        int info = 0;
        int offline = 0;

        for (AlertResponse alert : scopedAlerts) {
            if ("CRITICAL".equalsIgnoreCase(alert.severity())) {
                critical++;
            } else if ("WARNING".equalsIgnoreCase(alert.severity())) {
                warning++;
            } else if ("INFO".equalsIgnoreCase(alert.severity())) {
                info++;
            } else if ("OFFLINE".equalsIgnoreCase(alert.severity())) {
                offline++;
            }
        }

        int total = scopedAlerts.size();
        return new AlertSummaryResponse(
                critical,
                percent(critical, total),
                warning,
                percent(warning, total),
                info,
                percent(info, total),
                offline,
                percent(offline, total),
                total
        );
    }

    private static int percent(int value, int total) {
        return total == 0 ? 0 : Math.round((value * 100.0f) / total);
    }

    public AlertResponse acknowledge(Long id) {
        return updateStatus(id, "Reconocida");
    }

    public AlertResponse resolve(Long id) {
        return updateStatus(id, "Resuelta");
    }

    public void delete(Long id) {
        AlertResponse alert = findRawById(id);
        alerts.remove(alert);
        saveAlerts();
    }

    private AlertResponse updateStatus(Long id, String status) {
        AlertResponse alert = findRawById(id);
        if (!isProtocolEnabled(alert)) {
            throw new ApiException("Alerta no encontrada");
        }
        AlertResponse updated = new AlertResponse(
                alert.id(),
                alert.companyId(),
                alert.type(),
                alert.severity(),
                alert.title(),
                alert.description(),
                alert.vehicleLabel(),
                alert.vehicleCode(),
                alert.occurredAtLabel(),
                status,
                alert.duration(),
                currentIconFor(alert, alert.icon())
        );
        alerts.set(alerts.indexOf(alert), updated);
        saveAlerts();
        return withCurrentProtocolIcon(updated);
    }

    public AlertResponse recordMqttAlert(Long companyId, String type, String severity, String title, String description, String vehicleLabel, String vehicleCode) {
        return recordMqttAlert(companyId, type, severity, title, description, vehicleLabel, vehicleCode, null);
    }

    public AlertResponse recordMqttAlert(Long companyId, String type, String severity, String title, String description, String vehicleLabel, String vehicleCode, String icon) {
        AlertResponse existing = alerts.stream()
                .filter(alert -> alert.companyId().equals(companyId))
                .filter(alert -> alert.type().equalsIgnoreCase(type))
                .filter(alert -> alert.vehicleCode().equalsIgnoreCase(vehicleCode))
                .filter(alert -> !alert.status().equalsIgnoreCase("Resuelta"))
                .findFirst()
                .orElse(null);

        AlertResponse next = new AlertResponse(
                existing == null ? nextId() : existing.id(),
                companyId,
                type,
                severity,
                title,
                description,
                vehicleLabel,
                vehicleCode,
                "Ahora",
                "Activa",
                existing == null ? "0 min" : existing.duration(),
                currentIconFor(companyId, type, icon == null && existing != null ? existing.icon() : icon)
        );

        if (existing == null) {
            alerts.add(0, next);
        } else {
            alerts.set(alerts.indexOf(existing), next);
        }

        saveAlerts();
        return next;
    }

    public void resolveMqttAlert(Long companyId, String type, String vehicleCode) {
        boolean changed = false;
        for (int index = 0; index < alerts.size(); index++) {
            AlertResponse alert = alerts.get(index);
            if (alert.companyId().equals(companyId)
                    && alert.type().equalsIgnoreCase(type)
                    && alert.vehicleCode().equalsIgnoreCase(vehicleCode)
                    && !alert.status().equalsIgnoreCase("Resuelta")) {
                alerts.set(index, new AlertResponse(
                        alert.id(),
                        alert.companyId(),
                        alert.type(),
                        alert.severity(),
                        alert.title(),
                        alert.description(),
                        alert.vehicleLabel(),
                        alert.vehicleCode(),
                        alert.occurredAtLabel(),
                        "Resuelta",
                        alert.duration(),
                        currentIconFor(alert, alert.icon())
                ));
                changed = true;
            }
        }

        if (changed) {
            saveAlerts();
        }
    }

    private AlertResponse withCurrentProtocolIcon(AlertResponse alert) {
        String currentIcon = currentIconFor(alert, alert.icon());
        if (currentIcon.equals(alert.icon())) {
            return alert;
        }
        return new AlertResponse(
                alert.id(),
                alert.companyId(),
                alert.type(),
                alert.severity(),
                alert.title(),
                alert.description(),
                alert.vehicleLabel(),
                alert.vehicleCode(),
                alert.occurredAtLabel(),
                alert.status(),
                alert.duration(),
                currentIcon
        );
    }

    private String currentIconFor(AlertResponse alert, String fallbackIcon) {
        return currentIconFor(alert.companyId(), alert.type(), fallbackIcon);
    }

    private String currentIconFor(Long companyId, String type, String fallbackIcon) {
        return protocolConfigService.alertIconForType(companyId, type, iconOrDefault(fallbackIcon, type));
    }

    private String iconOrDefault(String icon, String type) {
        if (icon != null && !icon.isBlank()) {
            return icon;
        }
        String normalizedType = type == null ? "" : type.toUpperCase(java.util.Locale.ROOT);
        if (normalizedType.contains("DOOR")) return "fa-solid fa-door-open";
        if (normalizedType.contains("COOL")) return "fa-regular fa-snowflake";
        if (normalizedType.contains("NETWORK") || normalizedType.contains("OFFLINE")) return "fa-solid fa-wifi";
        if (normalizedType.contains("SENSOR")) return "fa-solid fa-circle-info";
        if (normalizedType.contains("FUEL")) return "fa-solid fa-gas-pump";
        if (normalizedType.contains("SPEED")) return "fa-solid fa-gauge-high";
        if (normalizedType.contains("HUMIDITY")) return "fa-solid fa-droplet";
        if (normalizedType.contains("BATTERY")) return "fa-solid fa-battery-half";
        return "fa-solid fa-temperature-half";
    }

    private Long nextId() {
        return alerts.stream().mapToLong(AlertResponse::id).max().orElse(0L) + 1;
    }

    private boolean isProtocolEnabled(AlertResponse alert) {
        return protocolConfigService.isEventTypeEnabled(alert.companyId(), alert.type());
    }

    private boolean matches(String expected, String actual) {
        return isBlank(expected) || "ALL".equalsIgnoreCase(expected) || normalize(actual).equals(normalize(expected));
    }

    private boolean contains(String value, String term) {
        return normalize(value).contains(normalize(term));
    }

    private String searchText(AlertResponse alert) {
        return alert.title() + " " + alert.description() + " " + alert.vehicleLabel() + " " + alert.vehicleCode() + " " + alert.type() + " " + alert.severity() + " " + alert.status();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void loadAlerts() {
        alerts.addAll(jsonStoreService.read(
                "alerts",
                storePath,
                new TypeReference<List<AlertResponse>>() {},
                this::defaultAlerts,
                "No se pudo cargar alertas persistidas",
                "No se pudo persistir alertas"
        ));
    }



    private void saveAlerts() {
        jsonStoreService.write("alerts", storePath, alerts, "No se pudo persistir alertas");
    }

    private List<AlertResponse> defaultAlerts() {
        return List.of(
                new AlertResponse(1L, 1L, "TEMPERATURE", "CRITICAL", "Temperatura alta", "La temperatura supero el limite permitido", "Camion 12 - ABC123", "ABC123", "Hoy, 10:32", "Activa", "25 min", "fa-solid fa-temperature-half"),
                new AlertResponse(2L, 1L, "DOOR", "WARNING", "Puerta abierta", "Puerta del compartimiento abierta", "Camion 07 - DEF456", "DEF456", "Hoy, 10:30", "Activa", "15 min", "fa-solid fa-door-open"),
                new AlertResponse(3L, 1L, "COOLING", "CRITICAL", "Equipo de frio apagado", "El equipo de frio no esta funcionando", "Camion 03 - GHI789", "GHI789", "Hoy, 10:28", "Activa", "32 min", "fa-regular fa-snowflake"),
                new AlertResponse(4L, 1L, "TEMPERATURE", "WARNING", "Temperatura fuera de rango", "Temperatura fuera del rango permitido", "Camion 02 - BBB222", "BBB222", "Hoy, 09:45", "Activa", "1 h 10 min", "fa-solid fa-temperature-half"),
                new AlertResponse(5L, 1L, "NETWORK", "OFFLINE", "Sin comunicacion", "Sin datos del vehiculo", "Camion 21 - MNO321", "MNO321", "Hoy, 09:20", "Activa", "2 h 45 min", "fa-solid fa-wifi"),
                new AlertResponse(6L, 1L, "SENSOR", "INFO", "Sensor reconectado", "Sensor de temperatura reconectado", "Camion 15 - JKL456", "JKL456", "Hoy, 08:50", "Informativa", "--", "fa-solid fa-circle-info"),
                new AlertResponse(7L, 1L, "DOOR", "WARNING", "Puerta entreabierta", "La puerta estuvo entreabierta", "Camion 08 - PQR678", "PQR678", "Hoy, 08:15", "Resuelta", "5 min", "fa-solid fa-door-open"),
                new AlertResponse(8L, 2L, "TEMPERATURE", "WARNING", "Temperatura fuera de rango", "Temperatura fuera del rango permitido", "Camion Norte 01 - NOR111", "NOR111", "Hoy, 09:00", "Activa", "40 min", "fa-solid fa-temperature-half")
        );
    }
}
