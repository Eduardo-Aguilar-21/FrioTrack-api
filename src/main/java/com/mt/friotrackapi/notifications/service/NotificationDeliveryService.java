package com.mt.friotrackapi.notifications.service;

import com.mt.friotrackapi.alerts.entity.AlertEntity;
import com.mt.friotrackapi.mobile.service.MobilePushNotificationService;
import com.mt.friotrackapi.notificationgroups.entity.NotificationGroupEntity;
import com.mt.friotrackapi.notifications.dto.NotificationDeliveryResponse;
import com.mt.friotrackapi.notifications.entity.NotificationDeliveryEntity;
import com.mt.friotrackapi.users.entity.UserEntity;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationDeliveryService {

    private final MobilePushNotificationService mobilePushNotificationService;

    public NotificationDeliveryService(MobilePushNotificationService mobilePushNotificationService) {
        this.mobilePushNotificationService = mobilePushNotificationService;
    }

    @PersistenceContext
    private EntityManager entityManager;


    public List<NotificationDeliveryResponse> findAll(Long companyId, Long userId, Long alertId) {
        List<NotificationDeliveryEntity> deliveries = entityManager.createQuery(
                "select d from NotificationDeliveryEntity d where d.alert.company.id = :companyId " +
                        "and (:userId is null or d.user.id = :userId) " +
                        "and (:alertId is null or d.alert.id = :alertId) order by d.id desc",
                NotificationDeliveryEntity.class)
                .setParameter("companyId", companyId)
                .setParameter("userId", userId)
                .setParameter("alertId", alertId)
                .getResultList();
        return deliveries.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void dispatchForAlert(AlertEntity alert) {
        List<NotificationGroupEntity> groups = entityManager.createQuery(
                "select distinct g from NotificationGroupEntity g left join fetch g.users left join fetch g.vehicles where g.company.id = :companyId and upper(g.status) = 'ACTIVE'",
                NotificationGroupEntity.class)
                .setParameter("companyId", alert.getCompany().getId())
                .getResultList();

        boolean shouldPushMobile = false;
        for (NotificationGroupEntity group : groups) {
            if (!matches(group, alert)) {
                continue;
            }

            for (UserEntity user : group.getUsers()) {
                if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    continue;
                }
                for (String channel : split(group.getChannels())) {
                    createIfMissing(alert, group, user, channel);
                    if ("APP".equalsIgnoreCase(channel)) {
                        shouldPushMobile = true;
                    }
                }
            }
        }
        if (shouldPushMobile) {
            mobilePushNotificationService.sendForAlert(alert);
        }
    }

    @Transactional
    public void markRead(Long alertId, Long userId) {
        entityManager.createQuery("select d from NotificationDeliveryEntity d where d.alert.id = :alertId and d.user.id = :userId", NotificationDeliveryEntity.class)
                .setParameter("alertId", alertId)
                .setParameter("userId", userId)
                .getResultList()
                .forEach(NotificationDeliveryEntity::markRead);
    }


    private NotificationDeliveryResponse toResponse(NotificationDeliveryEntity delivery) {
        return new NotificationDeliveryResponse(
                delivery.getId(),
                delivery.getAlert().getCompany().getId(),
                delivery.getAlert().getId(),
                delivery.getAlert().getTitle(),
                delivery.getAlert().getType(),
                delivery.getAlert().getSeverity(),
                delivery.getGroup().getId(),
                delivery.getGroup().getName(),
                delivery.getUser().getId(),
                delivery.getUser().getName(),
                delivery.getChannel(),
                delivery.getStatus(),
                delivery.getDeliveredAt(),
                delivery.getReadAt(),
                delivery.getFailureReason(),
                delivery.getCreatedAt()
        );
    }

    private boolean matches(NotificationGroupEntity group, AlertEntity alert) {
        if (!split(group.getAlertTypes()).contains(normalize(alert.getType()))) {
            return false;
        }
        if (!split(group.getSeverities()).contains(normalize(alert.getSeverity()))) {
            return false;
        }
        if (group.getVehicles().isEmpty()) {
            return true;
        }
        return group.getVehicles().stream().map(VehicleEntity::getCode).anyMatch(code -> code.equalsIgnoreCase(alert.getVehicleCode()));
    }

    private void createIfMissing(AlertEntity alert, NotificationGroupEntity group, UserEntity user, String channel) {
        Long existing = entityManager.createQuery("select count(d) from NotificationDeliveryEntity d where d.alert.id = :alertId and d.user.id = :userId and upper(d.channel) = :channel", Long.class)
                .setParameter("alertId", alert.getId())
                .setParameter("userId", user.getId())
                .setParameter("channel", normalize(channel))
                .getSingleResult();
        if (existing > 0) {
            return;
        }

        String normalizedChannel = normalize(channel);
        String status = "APP".equals(normalizedChannel) ? "DELIVERED" : "PENDING";
        Instant deliveredAt = "APP".equals(normalizedChannel) ? Instant.now() : null;
        String failureReason = "EMAIL".equals(normalizedChannel) ? "Pendiente de proveedor SMTP" : null;
        entityManager.persist(new NotificationDeliveryEntity(alert, group, user, normalizedChannel, status, deliveredAt, failureReason));
    }

    private Set<String> split(String value) {
        if (value == null || value.isBlank()) return Set.of();
        Set<String> values = new LinkedHashSet<>();
        Arrays.stream(value.split(",")).map(this::normalize).filter(item -> !item.isBlank()).forEach(values::add);
        return values;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
