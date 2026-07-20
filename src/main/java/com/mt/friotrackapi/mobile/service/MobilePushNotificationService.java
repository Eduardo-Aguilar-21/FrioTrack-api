package com.mt.friotrackapi.mobile.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.alerts.entity.AlertEntity;
import com.mt.friotrackapi.mobile.entity.MobilePushNotificationEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MobilePushNotificationService {
    private static final URI EXPO_PUSH_URL = URI.create("https://exp.host/--/api/v2/push/send");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final MobileDeviceService mobileDeviceService;

    @PersistenceContext
    private EntityManager entityManager;

    public MobilePushNotificationService(MobileDeviceService mobileDeviceService) {
        this.mobileDeviceService = mobileDeviceService;
    }

    @Transactional
    public void sendForAlert(AlertEntity alert) {
        List<MobileDeviceService.MobileDevice> devices = mobileDeviceService.activePushDevices(alert.getCompany().getId());
        for (MobileDeviceService.MobileDevice device : devices) {
            if (exists(alert.getId(), device.token())) continue;
            MobilePushNotificationEntity record = new MobilePushNotificationEntity(alert, alert.getCompany().getId(), device.token(), device.pushToken());
            entityManager.persist(record);
            entityManager.flush();
            send(record, device.pushToken(), alert);
        }
    }

    @Transactional
    public void markReceived(Long alertId, String mobileToken) {
        records(alertId, mobileToken).forEach(MobilePushNotificationEntity::received);
    }

    @Transactional
    public void markRead(Long alertId, String mobileToken) {
        records(alertId, mobileToken).forEach(MobilePushNotificationEntity::read);
    }

    private void send(MobilePushNotificationEntity record, String pushToken, AlertEntity alert) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to", pushToken);
            payload.put("sound", "default");
            payload.put("title", alert.getTitle());
            payload.put("body", alert.getVehicleLabel() + " · " + alert.getDescription());
            payload.put("data", Map.of("alertId", alert.getId(), "companyId", alert.getCompany().getId(), "type", alert.getType()));

            HttpRequest request = HttpRequest.newBuilder(EXPO_PUSH_URL)
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String ticketId = ticketId(response.body());
                record.sent(ticketId);
            } else {
                record.failed("Expo HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception ex) {
            record.failed(ex.getMessage());
        }
    }

    private String ticketId(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (data.isArray() && !data.isEmpty()) return data.get(0).path("id").asText(null);
            return data.path("id").asText(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean exists(Long alertId, String mobileToken) {
        Long count = entityManager.createQuery("select count(p) from MobilePushNotificationEntity p where p.alert.id = :alertId and p.mobileToken = :mobileToken", Long.class)
                .setParameter("alertId", alertId)
                .setParameter("mobileToken", mobileToken)
                .getSingleResult();
        return count > 0;
    }

    private List<MobilePushNotificationEntity> records(Long alertId, String mobileToken) {
        return entityManager.createQuery("select p from MobilePushNotificationEntity p where p.alert.id = :alertId and p.mobileToken = :mobileToken", MobilePushNotificationEntity.class)
                .setParameter("alertId", alertId)
                .setParameter("mobileToken", mobileToken)
                .getResultList();
    }
}
