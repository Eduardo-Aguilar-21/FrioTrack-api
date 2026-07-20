package com.mt.friotrackapi.alerts.service;

import com.mt.friotrackapi.alerts.dto.AlertResponse;
import com.mt.friotrackapi.alerts.dto.AlertSummaryResponse;
import com.mt.friotrackapi.alerts.entity.AlertEntity;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.notifications.service.NotificationDeliveryService;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.realtime.service.RealtimeEventService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlertService {

    private final ProtocolConfigService protocolConfigService;
    private final CompanyService companyService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final RealtimeEventService realtimeEventService;

    @PersistenceContext
    private EntityManager entityManager;

    public AlertService(ProtocolConfigService protocolConfigService, CompanyService companyService, NotificationDeliveryService notificationDeliveryService, RealtimeEventService realtimeEventService) {
        this.protocolConfigService = protocolConfigService;
        this.companyService = companyService;
        this.notificationDeliveryService = notificationDeliveryService;
        this.realtimeEventService = realtimeEventService;
    }

    public List<AlertResponse> findAll(Long companyId, String severity) {
        return findAll(companyId, severity, null, null, null, null);
    }

    public List<AlertResponse> findAll(Long companyId, String severity, String status, String type, String vehicle, String search) {
        String term = normalize(search);
        List<AlertEntity> source = companyId == null
                ? entityManager.createQuery("select a from AlertEntity a order by a.id desc", AlertEntity.class).getResultList()
                : entityManager.createQuery("select a from AlertEntity a where a.company.id = :companyId order by a.id desc", AlertEntity.class).setParameter("companyId", companyId).getResultList();
        return source.stream()
                .map(this::toResponse)
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
        AlertResponse alert = toResponse(entityById(id));
        if (!isProtocolEnabled(alert)) {
            throw new ApiException("Alerta no encontrada");
        }
        return withCurrentProtocolIcon(alert);
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

    @Transactional
    public AlertResponse acknowledge(Long id) {
        return updateStatus(id, "Reconocida");
    }

    @Transactional
    public AlertResponse resolve(Long id) {
        return updateStatus(id, "Resuelta");
    }

    @Transactional
    public void delete(Long id) {
        entityManager.remove(entityById(id));
    }

    private AlertResponse updateStatus(Long id, String status) {
        AlertEntity alert = entityById(id);
        AlertResponse current = toResponse(alert);
        if (!isProtocolEnabled(current)) {
            throw new ApiException("Alerta no encontrada");
        }
        alert.updateStatus(status);
        AlertResponse response = withCurrentProtocolIcon(toResponse(alert));
        realtimeEventService.publish(alert.getCompany().getId(), "alert", response);
        return response;
    }

    public AlertResponse recordMqttAlert(Long companyId, String type, String severity, String title, String description, String vehicleLabel, String vehicleCode) {
        return recordMqttAlert(companyId, type, severity, title, description, vehicleLabel, vehicleCode, null, null);
    }

    @Transactional
    public AlertResponse recordMqttAlert(Long companyId, String type, String severity, String title, String description, String vehicleLabel, String vehicleCode, String icon, String reading) {
        AlertEntity existing = activeMqttAlert(companyId, type, vehicleCode);

        String nextIcon = currentIconFor(companyId, type, icon == null && existing != null ? existing.getIcon() : icon);
        if (existing == null) {
            CompanyEntity company = companyService.entityById(companyId);
            AlertEntity next = new AlertEntity(
                    company,
                    type,
                    severity,
                    title,
                    description,
                    vehicleLabel,
                    vehicleCode,
                    "Ahora",
                    "Activa",
                    "0 min",
                    reading,
                    nextIcon
            );
            entityManager.persist(next);
            entityManager.flush();
            notificationDeliveryService.dispatchForAlert(next);
            AlertResponse response = withCurrentProtocolIcon(toResponse(next));
            realtimeEventService.publish(companyId, "alert", response);
            return response;
        }

        existing.updateMqtt(
                severity,
                title,
                description,
                vehicleLabel,
                "Ahora",
                "Activa",
                existing.getDuration(),
                reading,
                nextIcon
        );
        notificationDeliveryService.dispatchForAlert(existing);
        AlertResponse response = withCurrentProtocolIcon(toResponse(existing));
        realtimeEventService.publish(companyId, "alert", response);
        return response;
    }

    @Transactional
    public void resolveMqttAlert(Long companyId, String type, String vehicleCode) {
        List<AlertEntity> alerts = activeMqttAlerts(companyId, type, vehicleCode);
        alerts.forEach(alert -> {
            alert.updateStatus("Resuelta");
            realtimeEventService.publish(companyId, "alert", withCurrentProtocolIcon(toResponse(alert)));
        });
    }

    private AlertEntity activeMqttAlert(Long companyId, String type, String vehicleCode) {
        return activeMqttAlerts(companyId, type, vehicleCode).stream().findFirst().orElse(null);
    }

    private List<AlertEntity> activeMqttAlerts(Long companyId, String type, String vehicleCode) {
        return entityManager.createQuery("select a from AlertEntity a where a.company.id = :companyId and lower(a.type) = lower(:type) and lower(a.vehicleCode) = lower(:vehicleCode) and lower(a.status) <> lower(:resolved) order by a.id desc", AlertEntity.class)
                .setParameter("companyId", companyId)
                .setParameter("type", type)
                .setParameter("vehicleCode", vehicleCode)
                .setParameter("resolved", "Resuelta")
                .getResultList();
    }

    private AlertEntity entityById(Long id) {
        AlertEntity alert = entityManager.find(AlertEntity.class, id);
        if (alert == null) {
            throw new ApiException("Alerta no encontrada");
        }
        return alert;
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
                alert.reading(),
                currentIcon
        );
    }

    private AlertResponse toResponse(AlertEntity alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getCompany().getId(),
                alert.getType(),
                alert.getSeverity(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getVehicleLabel(),
                alert.getVehicleCode(),
                alert.getOccurredAtLabel(),
                alert.getStatus(),
                alert.getDuration(),
                alert.getReading(),
                alert.getIcon()
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
        return alert.title() + " " + alert.description() + " " + alert.vehicleLabel() + " " + alert.vehicleCode() + " " + alert.type() + " " + alert.severity() + " " + alert.status() + " " + (alert.reading() == null ? "" : alert.reading());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int percent(int value, int total) {
        return total == 0 ? 0 : Math.round((value * 100.0f) / total);
    }
}
