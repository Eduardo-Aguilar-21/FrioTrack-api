package com.mt.friotrackapi.protocol.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.protocol.dto.ProtocolConfigResponse;
import com.mt.friotrackapi.protocol.dto.ProtocolFieldConfigResponse;
import com.mt.friotrackapi.protocol.dto.SaveProtocolConfigRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProtocolConfigService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storePath = Path.of(System.getProperty("user.dir"), "data", "protocol-configs.json");
    private final Map<Long, StoredProtocolConfig> configs = new LinkedHashMap<>();
    private final CompanyService companyService;

    public ProtocolConfigService(CompanyService companyService) {
        this.companyService = companyService;
        loadConfigs();
    }

    public ProtocolConfigResponse findByCompany(Long companyId) {
        companyService.findById(companyId);
        StoredProtocolConfig config = configs.computeIfAbsent(companyId, this::defaultConfig);
        saveConfigs();
        return toResponse(config);
    }


    public boolean isFieldEnabled(Long companyId, String targetField) {
        if (targetField == null || targetField.isBlank()) {
            return true;
        }

        ProtocolConfigResponse config = findByCompany(companyId);
        return config.fields().stream()
                .filter(field -> targetField.equalsIgnoreCase(field.targetField()) || targetField.equalsIgnoreCase(field.key()))
                .findFirst()
                .map(ProtocolFieldConfigResponse::enabled)
                .orElse(true);
    }

    public boolean isEventTypeEnabled(Long companyId, String type) {
        String targetField = targetFieldForType(type);
        return targetField == null || isFieldEnabled(companyId, targetField);
    }

    public String targetFieldForType(String type) {
        if (type == null) {
            return null;
        }

        return switch (type.trim().toUpperCase()) {
            case "TEMPERATURE" -> "temperature";
            case "DOOR" -> "doorState";
            case "COOLING" -> "coolingUnitState";
            case "FUEL" -> "fuelLevel";
            case "SPEED" -> "speed";
            case "HUMIDITY" -> "humidity";
            default -> null;
        };
    }

    public ProtocolConfigResponse save(SaveProtocolConfigRequest request) {
        companyService.findById(request.companyId());
        StoredProtocolConfig config = new StoredProtocolConfig(
                request.companyId(),
                clean(request.brokerName()),
                clean(request.topicPattern()),
                cleanOptional(request.payloadRoot()),
                request.fields() == null ? List.of() : request.fields().stream().map(this::normalizeField).toList()
        );
        configs.put(request.companyId(), config);
        saveConfigs();
        return toResponse(config);
    }

    private ProtocolConfigResponse toResponse(StoredProtocolConfig config) {
        return new ProtocolConfigResponse(
                config.companyId(),
                config.brokerName(),
                config.topicPattern(),
                topicExample(config.topicPattern()),
                config.payloadRoot(),
                config.fields(),
                previewPayload(config.payloadRoot(), config.fields())
        );
    }

    private ProtocolFieldConfigResponse normalizeField(ProtocolFieldConfigResponse field) {
        return new ProtocolFieldConfigResponse(
                clean(field.key()),
                clean(field.label()),
                field.enabled(),
                clean(field.jsonPath()),
                normalizeDataType(field.dataType()),
                cleanOptional(field.unit()),
                cleanOptional(field.sampleValue()),
                clean(field.targetField())
        );
    }

    private Map<String, Object> previewPayload(String payloadRoot, List<ProtocolFieldConfigResponse> fields) {
        Map<String, Object> payload = new LinkedHashMap<>();

        for (ProtocolFieldConfigResponse field : fields) {
            if (!field.enabled() || field.jsonPath() == null || field.jsonPath().isBlank()) {
                continue;
            }

            putPath(payload, field.jsonPath(), sampleValue(field.sampleValue(), field.dataType()));
        }

        if (payloadRoot == null || payloadRoot.isBlank()) {
            return payload;
        }

        Map<String, Object> wrapped = new LinkedHashMap<>();
        putPath(wrapped, payloadRoot, payload);
        return wrapped;
    }

    @SuppressWarnings("unchecked")
    private void putPath(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;

        for (int index = 0; index < parts.length; index++) {
            String key = parts[index].trim();
            if (key.isBlank()) {
                continue;
            }

            if (index == parts.length - 1) {
                current.put(key, value);
                return;
            }

            Object next = current.get(key);
            if (!(next instanceof Map<?, ?>)) {
                next = new LinkedHashMap<String, Object>();
                current.put(key, next);
            }
            current = (Map<String, Object>) next;
        }
    }

    private Object sampleValue(String rawValue, String dataType) {
        String value = rawValue == null ? "" : rawValue.trim();
        return switch (normalizeDataType(dataType)) {
            case "NUMBER" -> parseNumber(value);
            case "BOOLEAN" -> Boolean.parseBoolean(value);
            default -> value;
        };
    }

    private Object parseNumber(String value) {
        if (value.isBlank()) {
            return 0;
        }

        try {
            double number = Double.parseDouble(value);
            if (Math.floor(number) == number) {
                return Long.valueOf((long) number);
            }
            return number;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String topicExample(String topicPattern) {
        String pattern = topicPattern == null || topicPattern.isBlank() ? "vehiculo/{id}" : topicPattern;
        return pattern.replace("{id}", "12");
    }

    private StoredProtocolConfig defaultConfig(Long companyId) {
        return new StoredProtocolConfig(
                companyId,
                "Broker interno MQTT",
                "vehiculo/{id}",
                "",
                List.of(
                        new ProtocolFieldConfigResponse("temperature", "Temperatura", true, "temperatura", "NUMBER", "C", "4.8", "temperature"),
                        new ProtocolFieldConfigResponse("humidity", "Humedad", true, "humedad", "NUMBER", "%", "45", "humidity"),
                        new ProtocolFieldConfigResponse("doorState", "Puerta", true, "puerta", "STRING", "", "Cerrada", "doorState"),
                        new ProtocolFieldConfigResponse("coolingUnitState", "Equipo de frio", true, "equipoFrio", "STRING", "", "Encendido", "coolingUnitState"),
                        new ProtocolFieldConfigResponse("fuelLevel", "Combustible", true, "combustible", "NUMBER", "%", "65", "fuelLevel"),
                        new ProtocolFieldConfigResponse("speed", "Velocidad", true, "velocidad", "NUMBER", "km/h", "65", "speed"),
                        new ProtocolFieldConfigResponse("latitude", "Latitud", true, "ubicacion.lat", "NUMBER", "", "-12.0576", "latitude"),
                        new ProtocolFieldConfigResponse("longitude", "Longitud", true, "ubicacion.lng", "NUMBER", "", "-76.9649", "longitude"),
                        new ProtocolFieldConfigResponse("battery", "Bateria", false, "bateria", "NUMBER", "%", "92", "battery")
                )
        );
    }

    private void loadConfigs() {
        try {
            if (Files.exists(storePath)) {
                configs.putAll(objectMapper.readValue(storePath.toFile(), new TypeReference<Map<Long, StoredProtocolConfig>>() {}));
                return;
            }

            configs.put(1L, defaultConfig(1L));
            configs.put(2L, defaultConfig(2L));
            saveConfigs();
        } catch (IOException ex) {
            throw new ApiException("No se pudo cargar configuraciones de protocolo");
        }
    }

    private void saveConfigs() {
        try {
            Files.createDirectories(storePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), configs);
        } catch (IOException ex) {
            throw new ApiException("No se pudo persistir configuracion de protocolo");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeDataType(String dataType) {
        String normalized = clean(dataType).toUpperCase();
        return switch (normalized) {
            case "NUMBER", "BOOLEAN", "STRING" -> normalized;
            default -> "STRING";
        };
    }

    public record StoredProtocolConfig(
            Long companyId,
            String brokerName,
            String topicPattern,
            String payloadRoot,
            List<ProtocolFieldConfigResponse> fields
    ) {
    }
}
