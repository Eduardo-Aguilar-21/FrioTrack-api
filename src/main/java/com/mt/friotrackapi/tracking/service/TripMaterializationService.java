package com.mt.friotrackapi.tracking.service;

import com.mt.friotrackapi.vehicles.service.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TripMaterializationService {
    private static final Logger log = LoggerFactory.getLogger(TripMaterializationService.class);
    private final TrackingService trackingService;
    private final VehicleService vehicleService;
    public TripMaterializationService(TrackingService trackingService, VehicleService vehicleService) {
        this.trackingService = trackingService;
        this.vehicleService = vehicleService;
    }

    @Scheduled(fixedDelayString = "${friotrack.trips.materialize-delay-ms:60000}", initialDelayString = "${friotrack.trips.materialize-initial-delay-ms:30000}")
    public void materialize() {
        vehicleService.findAll(null).forEach(vehicle -> {
            try { trackingService.trips(vehicle.id(), 2); }
            catch (Exception ex) { log.warn("No se pudo materializar viajes del vehiculo {}: {}", vehicle.id(), ex.getMessage()); }
        });
    }
}
