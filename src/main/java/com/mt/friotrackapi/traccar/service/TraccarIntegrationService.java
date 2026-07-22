package com.mt.friotrackapi.traccar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mt.friotrackapi.mqtt.service.MqttTelemetryIngestionService;
import com.mt.friotrackapi.traccar.dto.TraccarStatusResponse;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import com.mt.friotrackapi.vehicles.repository.VehicleRepository;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TraccarIntegrationService implements WebSocket.Listener {
    private static final Logger log = LoggerFactory.getLogger(TraccarIntegrationService.class);
    private static final double KNOTS_TO_KMH = 1.852;

    private final boolean enabled;
    private final String baseUrl;
    private final String email;
    private final String password;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VehicleRepository vehicleRepository;
    private final MqttTelemetryIngestionService telemetryIngestionService;
    private final TraccarFieldCatalogService fieldCatalogService;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<Long, String> imeiByTraccarDeviceId = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastPositionByDeviceId = new ConcurrentHashMap<>();
    private final Set<String> detectedPaths = ConcurrentHashMap.newKeySet();
    private final StringBuilder messageBuffer = new StringBuilder();

    private volatile WebSocket webSocket;
    private volatile String sessionCookie;
    private volatile String lastError;

    public TraccarIntegrationService(
            @Value("${friotrack.traccar.enabled:false}") boolean enabled,
            @Value("${friotrack.traccar.url:http://127.0.0.1:8082}") String baseUrl,
            @Value("${friotrack.traccar.email:}") String email,
            @Value("${friotrack.traccar.password:}") String password,
            VehicleRepository vehicleRepository,
            MqttTelemetryIngestionService telemetryIngestionService,
            TraccarFieldCatalogService fieldCatalogService
    ) {
        this.enabled = enabled;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.email = email;
        this.password = password;
        this.vehicleRepository = vehicleRepository;
        this.telemetryIngestionService = telemetryIngestionService;
        this.fieldCatalogService = fieldCatalogService;
        detectedPaths.addAll(List.of("latitude", "longitude", "speed", "altitude", "course", "deviceTime", "fixTime"));
    }

    @Scheduled(fixedDelayString = "${friotrack.traccar.reconnect-delay-ms:5000}")
    public synchronized void ensureConnected() {
        if (!enabled || connected()) return;
        if (email.isBlank() || password.isBlank()) {
            lastError = "Credenciales de Traccar no configuradas";
            return;
        }

        try {
            authenticate();
            refreshDevices();
            webSocket = httpClient.newWebSocketBuilder()
                    .header("Cookie", sessionCookie)
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(socketUri(), this)
                    .join();
            lastError = null;
            log.info("Integracion Traccar conectada con {} dispositivos", imeiByTraccarDeviceId.size());
        } catch (Exception ex) {
            webSocket = null;
            lastError = rootMessage(ex);
            log.warn("No se pudo conectar con Traccar: {}", lastError);
        }
    }

    public TraccarStatusResponse status(Long companyId) {
        List<String> paths = new ArrayList<>(companyId == null ? detectedPaths : fieldCatalogService.paths(companyId));
        paths.sort(Comparator.naturalOrder());
        return new TraccarStatusResponse(enabled, connected(), imeiByTraccarDeviceId.size(), paths, lastError);
    }

    @Override
    public void onOpen(WebSocket socket) {
        socket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
        synchronized (messageBuffer) {
            messageBuffer.append(data);
            if (last) {
                String message = messageBuffer.toString();
                messageBuffer.setLength(0);
                processMessage(message);
            }
        }
        socket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
        webSocket = null;
        lastError = "Conexion cerrada por Traccar: " + statusCode + " " + reason;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        webSocket = null;
        lastError = rootMessage(error);
        log.warn("Error en WebSocket Traccar: {}", lastError);
    }

    private void authenticate() throws Exception {
        String form = "email=" + encode(email) + "&password=" + encode(password);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/session"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Autenticacion Traccar HTTP " + response.statusCode());
        }
        sessionCookie = response.headers().firstValue("set-cookie")
                .map(value -> value.split(";", 2)[0])
                .orElseThrow(() -> new IllegalStateException("Traccar no devolvio cookie de sesion"));
    }

    private void refreshDevices() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/devices"))
                .timeout(Duration.ofSeconds(10))
                .header("Cookie", sessionCookie)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Consulta de dispositivos Traccar HTTP " + response.statusCode());
        }
        List<Long> latestPositionIds = new ArrayList<>();
        for (JsonNode device : objectMapper.readTree(response.body())) {
            imeiByTraccarDeviceId.put(device.path("id").asLong(), device.path("uniqueId").asText());
            long positionId = device.path("positionId").asLong();
            if (positionId > 0) latestPositionIds.add(positionId);
        }
        for (Long positionId : latestPositionIds) refreshPosition(positionId);
    }

    private void refreshPosition(Long positionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/positions?id=" + positionId))
                .timeout(Duration.ofSeconds(10))
                .header("Cookie", sessionCookie)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) return;
        for (JsonNode position : objectMapper.readTree(response.body())) processPosition(position);
    }

    private void processMessage(String message) {
        try {
            JsonNode positions = objectMapper.readTree(message).path("positions");
            if (!positions.isArray()) return;
            for (JsonNode position : positions) processPosition(position);
        } catch (Exception ex) {
            lastError = rootMessage(ex);
            log.warn("No se pudo procesar mensaje Traccar: {}", lastError);
        }
    }

    private void processPosition(JsonNode position) {
        long traccarDeviceId = position.path("deviceId").asLong();
        long positionId = position.path("id").asLong();
        if (positionId > 0 && positionId <= lastPositionByDeviceId.getOrDefault(traccarDeviceId, 0L)) return;

        String imei = imeiByTraccarDeviceId.get(traccarDeviceId);
        if (imei == null || imei.isBlank()) {
            lastError = "Dispositivo Traccar no identificado: " + traccarDeviceId;
            return;
        }
        VehicleEntity vehicle = vehicleRepository.findFirstByDeviceIdIgnoreCase(imei).orElse(null);
        if (vehicle == null) {
            lastError = "IMEI Traccar sin vehiculo Friotrack: " + imei;
            return;
        }

        String detectedProtocol = position.path("protocol").asText("teltonika").trim().toLowerCase(java.util.Locale.ROOT);
        if (detectedProtocol.isBlank()) detectedProtocol = "teltonika";
        vehicle.setDetectedProtocol(detectedProtocol);
        vehicleRepository.save(vehicle);

        collectDetectedPaths(position);
        fieldCatalogService.record(vehicle, position);
        String rawPayload = position.toString();
        ObjectNode mappingPayload = position.deepCopy();
        ObjectNode location = mappingPayload.putObject("ubicacion");
        if (position.path("latitude").isNumber()) {
            mappingPayload.put("latitud", position.path("latitude").asDouble());
            location.put("lat", position.path("latitude").asDouble());
        }
        if (position.path("longitude").isNumber()) {
            mappingPayload.put("longitud", position.path("longitude").asDouble());
            location.put("lng", position.path("longitude").asDouble());
        }
        if (position.path("speed").isNumber()) {
            double speedKmh = position.path("speed").asDouble() * KNOTS_TO_KMH;
            mappingPayload.put("speed", speedKmh);
            mappingPayload.put("velocidad", speedKmh);
        }
        JsonNode attributes = position.path("attributes");
        if (attributes.path("ignition").isBoolean()) mappingPayload.put("encendido", attributes.path("ignition").asBoolean());
        telemetryIngestionService.ingestVehicle(vehicle.getId(), detectedProtocol, mappingPayload.toString(), rawPayload);
        if (positionId > 0) lastPositionByDeviceId.put(traccarDeviceId, positionId);
        lastError = null;
    }

    private void collectDetectedPaths(JsonNode position) {
        position.fieldNames().forEachRemaining(field -> {
            if (!"attributes".equals(field)) detectedPaths.add(field);
        });
        JsonNode attributes = position.path("attributes");
        if (attributes.isObject()) {
            attributes.fieldNames().forEachRemaining(field -> detectedPaths.add("attributes." + field));
        }
    }

    private boolean connected() {
        WebSocket socket = webSocket;
        return socket != null && !socket.isInputClosed() && !socket.isOutputClosed();
    }

    private URI socketUri() {
        URI uri = URI.create(baseUrl);
        String scheme = "https".equalsIgnoreCase(uri.getScheme()) ? "wss" : "ws";
        return URI.create(scheme + "://" + uri.getAuthority() + "/api/socket");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
