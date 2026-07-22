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
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MobilePushNotificationService {
    private static final URI EXPO_PUSH_URL = URI.create("https://exp.host/--/api/v2/push/send");
    private static final URI EXPO_RECEIPTS_URL = URI.create("https://exp.host/--/api/v2/push/getReceipts");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final MobileDeviceService mobileDeviceService;
    private final String expoAccessToken;

    @PersistenceContext
    private EntityManager entityManager;

    public MobilePushNotificationService(MobileDeviceService mobileDeviceService, @Value("${friotrack.expo.access-token:}") String expoAccessToken) {
        this.mobileDeviceService = mobileDeviceService;
        this.expoAccessToken = expoAccessToken == null ? "" : expoAccessToken.trim();
    }

    @Transactional
    public void sendForAlert(AlertEntity alert, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        List<MobileDeviceService.MobileDevice> devices = mobileDeviceService.activePushDevices(alert.getCompany().getId(), userIds);
        for (MobileDeviceService.MobileDevice device : devices) {
            if (exists(alert.getId(), device.token())) continue;
            MobilePushNotificationEntity record = new MobilePushNotificationEntity(alert, alert.getCompany().getId(), device.userId(), device.token(), device.pushToken());
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

            HttpRequest.Builder builder = HttpRequest.newBuilder(EXPO_PUSH_URL).timeout(Duration.ofSeconds(8)).header("Content-Type", "application/json");
            if (!expoAccessToken.isBlank()) builder.header("Authorization", "Bearer " + expoAccessToken);
            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload))).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode ticket = ticket(response.body());
                if ("ok".equalsIgnoreCase(ticket.path("status").asText()) && ticket.hasNonNull("id")) record.sent(ticket.path("id").asText());
                else record.failed(ticket.path("message").asText("Expo no devolvio ticket"));
            } else {
                record.failed("Expo HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception ex) {
            record.failed(ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${friotrack.expo.receipt-delay-ms:60000}", initialDelayString = "${friotrack.expo.receipt-initial-delay-ms:60000}")
    @Transactional
    public void reconcileReceipts() {
        List<MobilePushNotificationEntity> pending = entityManager.createQuery("select p from MobilePushNotificationEntity p where p.status = 'SENT' and p.ticketId is not null order by p.id", MobilePushNotificationEntity.class).setMaxResults(100).getResultList();
        if (pending.isEmpty()) return;
        try {
            Map<String, Object> body = Map.of("ids", pending.stream().map(MobilePushNotificationEntity::getTicketId).toList());
            HttpRequest.Builder builder = HttpRequest.newBuilder(EXPO_RECEIPTS_URL).timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json");
            if (!expoAccessToken.isBlank()) builder.header("Authorization", "Bearer " + expoAccessToken);
            HttpResponse<String> response = httpClient.send(builder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) return;
            JsonNode receipts = objectMapper.readTree(response.body()).path("data");
            for (MobilePushNotificationEntity record : pending) {
                JsonNode receipt = receipts.path(record.getTicketId());
                if (receipt.isMissingNode()) continue;
                if ("ok".equalsIgnoreCase(receipt.path("status").asText())) record.delivered();
                else {
                    String error = receipt.path("details").path("error").asText(receipt.path("message").asText("Expo rechazo la notificacion"));
                    record.receiptFailed(error);
                    if ("DeviceNotRegistered".equalsIgnoreCase(error)) mobileDeviceService.invalidatePushToken(record.getPushToken());
                }
            }
        } catch (Exception ignored) { }
    }

    @Scheduled(fixedDelayString = "${friotrack.expo.retry-delay-ms:300000}", initialDelayString = "${friotrack.expo.retry-initial-delay-ms:120000}")
    @Transactional
    public void retryTransientFailures() {
        entityManager.createQuery("select p from MobilePushNotificationEntity p where p.status = 'FAILED' and p.retryCount < 3 and (p.failureReason is null or p.failureReason <> 'DeviceNotRegistered') order by p.id", MobilePushNotificationEntity.class)
                .setMaxResults(50).getResultList().forEach(record -> send(record, record.getPushToken(), record.getAlert()));
    }

    private JsonNode ticket(String body) {
        try {
            JsonNode data = objectMapper.readTree(body).path("data");
            return data.isArray() && !data.isEmpty() ? data.get(0) : data;
        } catch (Exception ex) {
            return objectMapper.createObjectNode().put("status", "error").put("message", "Respuesta Expo invalida");
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
