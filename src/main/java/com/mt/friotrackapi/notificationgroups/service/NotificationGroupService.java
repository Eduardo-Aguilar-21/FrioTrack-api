package com.mt.friotrackapi.notificationgroups.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.notificationgroups.dto.CreateNotificationGroupRequest;
import com.mt.friotrackapi.protocol.dto.ProtocolFieldConfigResponse;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.notificationgroups.dto.NotificationGroupResponse;
import com.mt.friotrackapi.notificationgroups.entity.NotificationGroupEntity;
import com.mt.friotrackapi.notificationgroups.repository.NotificationGroupRepository;
import com.mt.friotrackapi.users.entity.UserEntity;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationGroupService {

    private static final Set<String> ALLOWED_SEVERITIES = Set.of("CRITICAL", "WARNING", "INFO", "OFFLINE");
    private static final Set<String> ALLOWED_CHANNELS = Set.of("APP", "EMAIL");

    private final NotificationGroupRepository notificationGroupRepository;
    private final CompanyService companyService;
    private final ProtocolConfigService protocolConfigService;

    @PersistenceContext
    private EntityManager entityManager;

    public NotificationGroupService(NotificationGroupRepository notificationGroupRepository, CompanyService companyService, ProtocolConfigService protocolConfigService) {
        this.notificationGroupRepository = notificationGroupRepository;
        this.companyService = companyService;
        this.protocolConfigService = protocolConfigService;
    }

    public List<NotificationGroupResponse> findAll(Long companyId) {
        List<NotificationGroupEntity> groups = companyId == null
                ? notificationGroupRepository.findAll().stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList()
                : notificationGroupRepository.findByCompanyIdOrderByIdAsc(companyId);
        return groups.stream().map(this::toResponse).toList();
    }

    public NotificationGroupResponse findById(Long id) {
        return toResponse(entityById(id));
    }

    @Transactional
    public NotificationGroupResponse create(CreateNotificationGroupRequest request) {
        Long companyId = request.companyId();
        if (notificationGroupRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name().trim())) {
            throw new ApiException("Ya existe un grupo de notificaciones con ese nombre");
        }
        CompanyEntity company = companyService.entityById(companyId);
        NotificationGroupEntity group = new NotificationGroupEntity(
                company,
                request.name().trim(),
                clean(request.description()),
                join(validateList(request.alertTypes(), allowedAlertTypes(companyId), "tipo de alerta")),
                join(validateList(request.severities(), ALLOWED_SEVERITIES, "severidad")),
                join(validateList(request.channels(), ALLOWED_CHANNELS, "canal")),
                normalizeStatus(request.status())
        );
        applyMembers(group, companyId, request.userIds(), request.vehicleIds());
        return toResponse(notificationGroupRepository.save(group));
    }

    @Transactional
    public NotificationGroupResponse update(Long id, CreateNotificationGroupRequest request) {
        NotificationGroupEntity group = entityById(id);
        Long companyId = request.companyId();
        if (notificationGroupRepository.existsByCompanyIdAndNameIgnoreCaseAndIdNot(companyId, request.name().trim(), id)) {
            throw new ApiException("Ya existe un grupo de notificaciones con ese nombre");
        }
        CompanyEntity company = companyService.entityById(companyId);
        group.update(
                company,
                request.name().trim(),
                clean(request.description()),
                join(validateList(request.alertTypes(), allowedAlertTypes(companyId), "tipo de alerta")),
                join(validateList(request.severities(), ALLOWED_SEVERITIES, "severidad")),
                join(validateList(request.channels(), ALLOWED_CHANNELS, "canal")),
                normalizeStatus(request.status())
        );
        applyMembers(group, companyId, request.userIds(), request.vehicleIds());
        return toResponse(group);
    }

    @Transactional
    public NotificationGroupResponse setStatus(Long id, String status) {
        NotificationGroupEntity group = entityById(id);
        group.setStatus(normalizeStatus(status));
        return toResponse(group);
    }

    @Transactional
    public void delete(Long id) {
        notificationGroupRepository.delete(entityById(id));
    }

    public NotificationGroupEntity entityById(Long id) {
        return notificationGroupRepository.findById(id).orElseThrow(() -> new ApiException("Grupo de notificaciones no encontrado"));
    }

    private void applyMembers(NotificationGroupEntity group, Long companyId, List<Long> userIds, List<Long> vehicleIds) {
        group.getUsers().clear();
        for (Long userId : uniqueIds(userIds)) {
            UserEntity user = entityManager.find(UserEntity.class, userId);
            if (user == null || !companyId.equals(user.getCompany().getId())) {
                throw new ApiException("Usuario fuera de la empresa del grupo");
            }
            group.getUsers().add(user);
        }

        group.getVehicles().clear();
        for (Long vehicleId : uniqueIds(vehicleIds)) {
            VehicleEntity vehicle = entityManager.find(VehicleEntity.class, vehicleId);
            if (vehicle == null || !companyId.equals(vehicle.getCompany().getId())) {
                throw new ApiException("Vehiculo fuera de la empresa del grupo");
            }
            group.getVehicles().add(vehicle);
        }
    }

    private List<Long> uniqueIds(List<Long> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(id -> id != null && id > 0).distinct().toList();
    }


    private Set<String> allowedAlertTypes(Long companyId) {
        Set<String> allowed = new LinkedHashSet<>();
        allowed.add("OFFLINE");
        protocolConfigService.findAllByCompany(companyId).forEach(config -> {
            for (ProtocolFieldConfigResponse field : config.fields()) {
                if (field.enabled() && field.alertMode() != null && !field.alertMode().equalsIgnoreCase("NONE")) {
                    allowed.add(alertTypeForField(field));
                }
            }
        });
        return allowed;
    }

    private String alertTypeForField(ProtocolFieldConfigResponse field) {
        String targetField = field.targetField() == null || field.targetField().isBlank() ? field.key() : field.targetField();
        if ("temperature".equalsIgnoreCase(targetField)) return "TEMPERATURE";
        if ("doorState".equalsIgnoreCase(targetField)) return "DOOR";
        if ("coolingUnitState".equalsIgnoreCase(targetField)) return "COOLING";
        return "CUSTOM_" + targetField.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> validateList(Collection<String> values, Set<String> allowed, String label) {
        if (values == null || values.isEmpty()) {
            throw new ApiException("Debes seleccionar al menos un " + label);
        }
        List<String> normalized = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalized.isEmpty() || !allowed.containsAll(normalized)) {
            throw new ApiException("Seleccion invalida para " + label);
        }
        return normalized;
    }

    private String normalizeStatus(String value) {
        String normalized = value == null ? "ACTIVE" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ACTIVE") && !normalized.equals("INACTIVE")) {
            throw new ApiException("Estado invalido para grupo de notificaciones");
        }
        return normalized;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String join(List<String> values) {
        return String.join(",", values);
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private NotificationGroupResponse toResponse(NotificationGroupEntity group) {
        return new NotificationGroupResponse(
                group.getId(),
                group.getCompany().getId(),
                group.getCompany().getName(),
                group.getName(),
                group.getDescription(),
                split(group.getAlertTypes()),
                split(group.getSeverities()),
                split(group.getChannels()),
                group.getUsers().stream().map(UserEntity::getId).toList(),
                group.getUsers().stream().map(UserEntity::getName).toList(),
                group.getVehicles().stream().map(VehicleEntity::getId).toList(),
                group.getVehicles().stream().map(VehicleEntity::getLabel).toList(),
                group.getStatus()
        );
    }
}
