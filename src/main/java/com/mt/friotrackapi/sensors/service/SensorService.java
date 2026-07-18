package com.mt.friotrackapi.sensors.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.sensors.dto.CreateSensorRequest;
import com.mt.friotrackapi.sensors.dto.SensorResponse;
import com.mt.friotrackapi.sensors.entity.SensorParameterEntity;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SensorService {

    private final CompanyService companyService;
    private final VehicleService vehicleService;

    @PersistenceContext
    private EntityManager entityManager;

    public SensorService(CompanyService companyService, VehicleService vehicleService) {
        this.companyService = companyService;
        this.vehicleService = vehicleService;
    }

    public List<SensorResponse> findAll(Long companyId) {
        if (companyId == null) {
            return entityManager.createQuery("select s from SensorParameterEntity s order by s.id", SensorParameterEntity.class).getResultList().stream().map(this::toResponse).toList();
        }

        companyService.findById(companyId);
        return entityManager.createQuery("select s from SensorParameterEntity s where s.company.id = :companyId order by s.id", SensorParameterEntity.class).setParameter("companyId", companyId).getResultList().stream().map(this::toResponse).toList();
    }

    public SensorResponse findById(Long id) {
        return toResponse(entityById(id));
    }

    @Transactional
    public SensorResponse create(CreateSensorRequest request) {
        CompanyEntity company = companyService.entityById(request.companyId());
        VehicleEntity vehicle = vehicleService.entityById(request.vehicleId());
        if (!vehicle.getCompany().getId().equals(request.companyId())) {
            throw new ApiException("El vehiculo no pertenece a la empresa");
        }

        if (countExisting(request.companyId(), request.code(), null) > 0) {
            throw new ApiException("El parametro ya existe en la empresa");
        }

        SensorParameterEntity sensor = new SensorParameterEntity(
                company,
                vehicle,
                request.code(),
                request.type(),
                request.unit(),
                "Sin lectura",
                "ACTIVE"
        );
        entityManager.persist(sensor);
        return toResponse(sensor);
    }

    @Transactional
    public SensorResponse update(Long id, CreateSensorRequest request) {
        SensorParameterEntity current = entityById(id);
        CompanyEntity company = companyService.entityById(request.companyId());
        VehicleEntity vehicle = vehicleService.entityById(request.vehicleId());
        if (!vehicle.getCompany().getId().equals(request.companyId())) {
            throw new ApiException("El vehiculo no pertenece a la empresa");
        }

        if (countExisting(request.companyId(), request.code(), id) > 0) {
            throw new ApiException("El parametro ya existe en la empresa");
        }

        current.update(company, vehicle, request.code(), request.type(), request.unit());
        return toResponse(current);
    }

    @Transactional
    public SensorResponse setStatus(Long id, String status) {
        SensorParameterEntity current = entityById(id);
        current.setStatus(normalizeStatus(status));
        return toResponse(current);
    }

    private SensorParameterEntity entityById(Long id) {
        SensorParameterEntity sensor = entityManager.find(SensorParameterEntity.class, id);
        if (sensor == null) {
            throw new ApiException("Parametro no encontrado");
        }
        return sensor;
    }

    private long countExisting(Long companyId, String code, Long excludedId) {
        String query = "select count(s) from SensorParameterEntity s where s.company.id = :companyId and lower(s.code) = lower(:code)";
        if (excludedId != null) {
            query += " and s.id <> :excludedId";
        }
        var typedQuery = entityManager.createQuery(query, Long.class)
                .setParameter("companyId", companyId)
                .setParameter("code", code);
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
        return normalized.equals("ACTIVE") ? "ACTIVE" : "INACTIVE";
    }

    private SensorResponse toResponse(SensorParameterEntity sensor) {
        return new SensorResponse(
                sensor.getId(),
                sensor.getCompany().getId(),
                sensor.getVehicle().getId(),
                sensor.getCode(),
                sensor.getVehicle().getLabel(),
                sensor.getType(),
                sensor.getUnit(),
                sensor.getLastValue(),
                sensor.getStatus()
        );
    }
}
