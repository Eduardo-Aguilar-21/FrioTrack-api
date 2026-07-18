package com.mt.friotrackapi.vehicles.service;

import com.mt.friotrackapi.alerts.service.AlertService;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleConnectivityMonitor {

    private final AlertService alertService;

    @PersistenceContext
    private EntityManager entityManager;

    public VehicleConnectivityMonitor(AlertService alertService) {
        this.alertService = alertService;
    }

    @Scheduled(fixedDelayString = "${friotrack.connectivity.check-delay-ms:60000}")
    @Transactional
    public void refreshConnectivity() {
        Instant now = Instant.now();
        List<VehicleEntity> vehicles = entityManager
                .createQuery("select v from VehicleEntity v join fetch v.company order by v.id", VehicleEntity.class)
                .getResultList();

        for (VehicleEntity vehicle : vehicles) {
            refreshVehicle(vehicle, now);
        }
    }

    private void refreshVehicle(VehicleEntity vehicle, Instant now) {
        Instant lastSeenAt = vehicle.getLastSeenAt();
        if (lastSeenAt == null) {
            markOffline(vehicle, "Nunca conectado", "Sin lecturas MQTT registradas");
            return;
        }

        long minutesWithoutSignal = Math.max(0, Duration.between(lastSeenAt, now).toMinutes());
        int warningMinutes = Math.max(1, vehicle.getCompany().getWarningOfflineMinutes());
        int criticalMinutes = Math.max(warningMinutes, vehicle.getCompany().getCriticalOfflineMinutes());

        if (minutesWithoutSignal >= criticalMinutes) {
            alertService.resolveMqttAlert(vehicle.getCompany().getId(), "NETWORK_WARNING", vehicle.getCode());
            markOffline(vehicle, minutesWithoutSignal + " min sin datos", "No se reciben paquetes MQTT desde hace " + minutesWithoutSignal + " min");
            return;
        }

        if (minutesWithoutSignal >= warningMinutes) {
            alertService.recordMqttAlert(
                    vehicle.getCompany().getId(),
                    "NETWORK_WARNING",
                    "WARNING",
                    "Comunicacion retrasada",
                    "No se reciben paquetes MQTT desde hace " + minutesWithoutSignal + " min",
                    vehicle.getLabel(),
                    vehicle.getCode(),
                    "fa-solid fa-signal",
                    minutesWithoutSignal + " min sin datos"
            );
            return;
        }

        alertService.resolveMqttAlert(vehicle.getCompany().getId(), "NETWORK_WARNING", vehicle.getCode());
        alertService.resolveMqttAlert(vehicle.getCompany().getId(), "NETWORK", vehicle.getCode());
    }

    private void markOffline(VehicleEntity vehicle, String reading, String description) {
        vehicle.setStatus("SIN_COMUNICACION");
        alertService.recordMqttAlert(
                vehicle.getCompany().getId(),
                "NETWORK",
                "OFFLINE",
                "Sin comunicacion",
                description,
                vehicle.getLabel(),
                vehicle.getCode(),
                "fa-solid fa-wifi",
                reading
        );
    }
}
