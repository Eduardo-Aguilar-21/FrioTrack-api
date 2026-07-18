package com.mt.friotrackapi.vehicles.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.protocol.dto.TemperatureRulesResponse;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.vehicles.dto.CreateVehicleRequest;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final CompanyService companyService;
    private final ProtocolConfigService protocolConfigService;

    @PersistenceContext
    private EntityManager entityManager;

    public VehicleService(CompanyService companyService, ProtocolConfigService protocolConfigService) {
        this.companyService = companyService;
        this.protocolConfigService = protocolConfigService;
    }

    public List<VehicleResponse> findAll(Long companyId) {
        if (companyId == null) {
            return entityManager.createQuery("select v from VehicleEntity v order by v.id", VehicleEntity.class).getResultList().stream().map(this::toResponse).toList();
        }

        companyService.findById(companyId);
        return entityManager.createQuery("select v from VehicleEntity v where v.company.id = :companyId order by v.id", VehicleEntity.class).setParameter("companyId", companyId).getResultList().stream().map(this::toResponse).toList();
    }

    public VehicleResponse findById(Long id) {
        return toResponse(entityById(id));
    }

    public VehicleEntity entityById(Long id) {
        VehicleEntity vehicle = entityManager.find(VehicleEntity.class, id);
        if (vehicle == null) {
            throw new ApiException("Vehiculo no encontrado");
        }
        return vehicle;
    }

    @Transactional
    public VehicleResponse create(CreateVehicleRequest request) {
        CompanyEntity company = companyService.entityById(request.companyId());
        boolean exists = countExisting(request.companyId(), request.code(), request.plate(), null) > 0;
        if (exists) {
            throw new ApiException("El vehiculo ya existe en la empresa");
        }

        VehicleEntity vehicle = new VehicleEntity(
                company,
                request.code(),
                request.plate(),
                request.label(),
                "SIN_COMUNICACION",
                request.driver(),
                request.imei(),
                request.model(),
                request.year(),
                request.unitType(),
                request.loadCapacityKg(),
                -12.0464,
                -77.0428,
                "--",
                "Sin datos",
                "Cerrada",
                "Encendido",
                "Nunca conectado"
        );
        entityManager.persist(vehicle);
        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse update(Long id, CreateVehicleRequest request) {
        VehicleEntity current = entityById(id);
        CompanyEntity company = companyService.entityById(request.companyId());
        boolean exists = countExisting(request.companyId(), request.code(), request.plate(), id) > 0;
        if (exists) {
            throw new ApiException("El vehiculo ya existe en la empresa");
        }

        current.updateIdentity(
                company,
                request.code(),
                request.plate(),
                request.label(),
                request.driver(),
                request.imei(),
                request.model(),
                request.year(),
                request.unitType(),
                request.loadCapacityKg()
        );
        return toResponse(current);
    }

    @Transactional
    public VehicleResponse setStatus(Long id, String status) {
        VehicleEntity current = entityById(id);
        current.setStatus(normalizeStatus(status));
        return toResponse(current);
    }

    @Transactional
    public VehicleResponse updateTelemetryState(
            Long id,
            Double latitude,
            Double longitude,
            String currentTemperature,
            String temperatureState,
            String doorState,
            String coolingUnitState,
            String lastCommunication
    ) {
        VehicleEntity current = entityById(id);
        Instant now = Instant.now();
        String nextStatus = vehicleStatus(current.getCompany().getId(), currentTemperature, temperatureState, current.getStatus());
        if ("SIN_COMUNICACION".equalsIgnoreCase(nextStatus) && currentTemperature == null) {
            nextStatus = "EN_RANGO";
        }
        current.updateTelemetry(latitude, longitude, currentTemperature, temperatureState, doorState, coolingUnitState, lastCommunication, now, nextStatus);
        return toResponse(current);
    }

    @Transactional
    public VehicleResponse markOffline(Long id) {
        VehicleEntity current = entityById(id);
        current.setStatus("SIN_COMUNICACION");
        return toResponse(current);
    }

    private long countExisting(Long companyId, String code, String plate, Long excludedId) {
        String query = "select count(v) from VehicleEntity v where v.company.id = :companyId and (lower(v.code) = lower(:code) or lower(v.plate) = lower(:plate))";
        if (excludedId != null) {
            query += " and v.id <> :excludedId";
        }
        var typedQuery = entityManager.createQuery(query, Long.class)
                .setParameter("companyId", companyId)
                .setParameter("code", code)
                .setParameter("plate", plate);
        if (excludedId != null) {
            typedQuery.setParameter("excludedId", excludedId);
        }
        return typedQuery.getSingleResult();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "INACTIVE";
        }
        String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.equals("ACTIVE") || normalized.equals("EN_RANGO") ? "EN_RANGO" : "INACTIVE";
    }

    private String vehicleStatus(Long companyId, String currentTemperature, String temperatureState, String currentStatus) {
        if (currentTemperature == null || currentTemperature.isBlank()) {
            return currentStatus;
        }
        Double value = parseTemperature(currentTemperature);
        if (value == null) {
            return currentStatus;
        }
        TemperatureRulesResponse rules = protocolConfigService.temperatureRules(companyId);
        if (value > rules.criticalHigh() || value < rules.criticalLow()) {
            return "CRITICO";
        }
        if (temperatureState != null && temperatureState.equalsIgnoreCase("Fuera de rango")) {
            return "ADVERTENCIA";
        }
        return "EN_RANGO";
    }

    private String lastCommunicationLabel(VehicleEntity vehicle) {
        Instant lastSeenAt = vehicle.getLastSeenAt();
        if (lastSeenAt == null) {
            return vehicle.getLastCommunication() == null || vehicle.getLastCommunication().isBlank() ? "Nunca conectado" : vehicle.getLastCommunication();
        }

        long minutes = Math.max(0, Duration.between(lastSeenAt, Instant.now()).toMinutes());
        if (minutes == 0) {
            return "Ahora";
        }
        if (minutes == 1) {
            return "Hace 1 min";
        }
        if (minutes < 60) {
            return "Hace " + minutes + " min";
        }
        long hours = minutes / 60;
        if (hours == 1) {
            return "Hace 1 h";
        }
        return "Hace " + hours + " h";
    }

    private Double parseTemperature(String value) {
        try {
            return Double.parseDouble(value.replace("°C", "").replace("C", "").trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private VehicleResponse toResponse(VehicleEntity vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getCompany().getId(),
                vehicle.getCode(),
                vehicle.getPlate(),
                vehicle.getLabel(),
                vehicle.getStatus(),
                vehicle.getDriver(),
                vehicle.getDeviceId(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getUnitType(),
                vehicle.getLoadCapacityKg(),
                vehicle.getLatitude(),
                vehicle.getLongitude(),
                vehicle.getCurrentTemperature(),
                vehicle.getTemperatureState(),
                vehicle.getDoorState(),
                vehicle.getCoolingUnitState(),
                lastCommunicationLabel(vehicle)
        );
    }
}
