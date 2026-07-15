package com.mt.friotrackapi.alerts.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.alerts.dto.AlertResponse;
import com.mt.friotrackapi.alerts.dto.AlertSummaryResponse;
import com.mt.friotrackapi.common.exception.ApiException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storePath = Path.of(System.getProperty("user.dir"), "data", "alerts.json");
    private final List<AlertResponse> alerts = new ArrayList<>();

    public AlertService() {
        loadAlerts();
    }

    public List<AlertResponse> findAll(Long companyId, String severity) {
        return alerts.stream()
                .filter(alert -> companyId == null || alert.companyId().equals(companyId))
                .filter(alert -> severity == null || severity.isBlank() || severity.equalsIgnoreCase("ALL") || alert.severity().equalsIgnoreCase(severity))
                .toList();
    }

    public AlertResponse findById(Long id) {
        return alerts.stream()
                .filter(alert -> alert.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException("Alerta no encontrada"));
    }

    public AlertSummaryResponse summary() {
        int critical = 0;
        int warning = 0;
        int info = 0;
        int offline = 0;

        for (AlertResponse alert : alerts) {
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

        return new AlertSummaryResponse(critical, warning, info, offline, alerts.size());
    }

    public AlertResponse resolve(Long id) {
        AlertResponse alert = findById(id);
        AlertResponse resolved = new AlertResponse(
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
                alert.duration()
        );
        alerts.set(alerts.indexOf(alert), resolved);
        saveAlerts();
        return resolved;
    }

    private void loadAlerts() {
        try {
            if (Files.exists(storePath)) {
                alerts.addAll(objectMapper.readValue(storePath.toFile(), new TypeReference<List<AlertResponse>>() {}));
                return;
            }

            alerts.addAll(defaultAlerts());
            saveAlerts();
        } catch (IOException ex) {
            throw new ApiException("No se pudo cargar alertas persistidas");
        }
    }

    private void saveAlerts() {
        try {
            Files.createDirectories(storePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), alerts);
        } catch (IOException ex) {
            throw new ApiException("No se pudo persistir alertas");
        }
    }

    private List<AlertResponse> defaultAlerts() {
        return List.of(
                new AlertResponse(1L, 1L, "TEMPERATURE", "CRITICAL", "Temperatura alta", "La temperatura supero el limite permitido", "Camion 12 - ABC123", "ABC123", "Hoy, 10:32", "Activa", "25 min"),
                new AlertResponse(2L, 1L, "DOOR", "WARNING", "Puerta abierta", "Puerta del compartimiento abierta", "Camion 07 - DEF456", "DEF456", "Hoy, 10:30", "Activa", "15 min"),
                new AlertResponse(3L, 1L, "COOLING", "CRITICAL", "Equipo de frio apagado", "El equipo de frio no esta funcionando", "Camion 03 - GHI789", "GHI789", "Hoy, 10:28", "Activa", "32 min"),
                new AlertResponse(4L, 1L, "TEMPERATURE", "WARNING", "Temperatura fuera de rango", "Temperatura fuera del rango permitido", "Camion 02 - BBB222", "BBB222", "Hoy, 09:45", "Activa", "1 h 10 min"),
                new AlertResponse(5L, 1L, "NETWORK", "OFFLINE", "Sin comunicacion", "Sin datos del vehiculo", "Camion 21 - MNO321", "MNO321", "Hoy, 09:20", "Activa", "2 h 45 min"),
                new AlertResponse(6L, 1L, "SENSOR", "INFO", "Sensor reconectado", "Sensor de temperatura reconectado", "Camion 15 - JKL456", "JKL456", "Hoy, 08:50", "Informativa", "--"),
                new AlertResponse(7L, 1L, "DOOR", "WARNING", "Puerta entreabierta", "La puerta estuvo entreabierta", "Camion 08 - PQR678", "PQR678", "Hoy, 08:15", "Resuelta", "5 min"),
                new AlertResponse(8L, 2L, "TEMPERATURE", "WARNING", "Temperatura fuera de rango", "Temperatura fuera del rango permitido", "Camion Norte 01 - NOR111", "NOR111", "Hoy, 09:00", "Activa", "40 min")
        );
    }
}
