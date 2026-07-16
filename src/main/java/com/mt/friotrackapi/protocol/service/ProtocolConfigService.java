package com.mt.friotrackapi.protocol.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.persistence.service.JsonStoreService;
import com.mt.friotrackapi.protocol.dto.ProtocolConfigResponse;
import com.mt.friotrackapi.protocol.dto.ProtocolFieldConfigResponse;
import com.mt.friotrackapi.protocol.dto.SaveProtocolConfigRequest;
import com.mt.friotrackapi.protocol.dto.TemperatureRulesResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProtocolConfigService {

    private final Path storePath = Path.of(System.getProperty("user.dir"), "data", "protocol-configs.json");
    private final Map<Long, StoredProtocolConfig> configs = new LinkedHashMap<>();
    private final CompanyService companyService;
    private final JsonStoreService jsonStoreService;

    public ProtocolConfigService(CompanyService companyService, JsonStoreService jsonStoreService) {
        this.companyService = companyService;
        this.jsonStoreService = jsonStoreService;
        loadConfigs();
    }

    public ProtocolConfigResponse findByCompany(Long companyId) {
        companyService.findById(companyId);
        StoredProtocolConfig config = configs.computeIfAbsent(companyId, this::defaultConfig);
        saveConfigs();
        return toResponse(config);
    }


    public TemperatureRulesResponse temperatureRules(Long companyId) {
        ProtocolConfigResponse config = findByCompany(companyId);
        ProtocolFieldConfigResponse temperature = fieldForTarget(config.fields(), "temperature");
        TemperatureRulesResponse fallback = config.temperatureRules();
        if (temperature != null && "RANGE".equalsIgnoreCase(clean(temperature.alertMode()))) {
            return new TemperatureRulesResponse(
                    temperature.alertMin() == null ? fallback.minAllowed() : temperature.alertMin(),
                    temperature.alertMax() == null ? fallback.maxAllowed() : temperature.alertMax(),
                    fallback.criticalLow(),
                    fallback.criticalHigh()
            );
        }
        return fallback;
    }

    public ProtocolFieldConfigResponse fieldForTarget(Long companyId, String targetField) {
        return fieldForTarget(findByCompany(companyId).fields(), targetField);
    }

    private ProtocolFieldConfigResponse fieldForTarget(List<ProtocolFieldConfigResponse> fields, String targetField) {
        if (fields == null || targetField == null) {
            return null;
        }
        return fields.stream()
                .filter(field -> targetField.equalsIgnoreCase(field.targetField()) || targetField.equalsIgnoreCase(field.key()))
                .findFirst()
                .orElse(null);
    }

    public String targetRangeLabel(Long companyId) {
        TemperatureRulesResponse rules = temperatureRules(companyId);
        return formatRuleNumber(rules.minAllowed()) + " °C a " + formatRuleNumber(rules.maxAllowed()) + " °C";
    }

    private TemperatureRulesResponse rulesOrDefault(TemperatureRulesResponse rules) {
        TemperatureRulesResponse defaults = defaultTemperatureRules();
        if (rules == null) {
            return defaults;
        }
        return new TemperatureRulesResponse(
                rules.minAllowed() == null ? defaults.minAllowed() : rules.minAllowed(),
                rules.maxAllowed() == null ? defaults.maxAllowed() : rules.maxAllowed(),
                rules.criticalLow() == null ? defaults.criticalLow() : rules.criticalLow(),
                rules.criticalHigh() == null ? defaults.criticalHigh() : rules.criticalHigh()
        );
    }

    private TemperatureRulesResponse defaultTemperatureRules() {
        return new TemperatureRulesResponse(-2.0, 5.0, -5.0, 8.0);
    }

    private String formatRuleNumber(Double value) {
        if (value == null) {
            return "--";
        }
        return Math.floor(value) == value ? String.format(java.util.Locale.US, "%.0f", value) : String.format(java.util.Locale.US, "%.1f", value);
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

    public String alertIconForType(Long companyId, String type, String fallbackIcon) {
        String targetField = targetFieldForType(type);
        if (targetField == null) {
            return fallbackIcon;
        }
        ProtocolFieldConfigResponse field = fieldForTarget(companyId, targetField);
        if (field == null || field.alertIcon() == null || field.alertIcon().isBlank()) {
            return fallbackIcon;
        }
        return field.alertIcon();
    }

    public String targetFieldForType(String type) {
        if (type == null) {
            return null;
        }

        String normalizedType = type.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalizedType.startsWith("CUSTOM_") && normalizedType.length() > "CUSTOM_".length()) {
            return normalizedType.substring("CUSTOM_".length());
        }

        return switch (normalizedType) {
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
                request.fields() == null ? List.of() : request.fields().stream().map(this::normalizeField).toList(),
                rulesFromFields(request.fields(), request.temperatureRules())
        );
        configs.put(request.companyId(), config);
        saveConfigs();
        return toResponse(config);
    }

    private ProtocolConfigResponse toResponse(StoredProtocolConfig config) {
        List<ProtocolFieldConfigResponse> fields = config.fields() == null
                ? defaultConfig(config.companyId()).fields()
                : config.fields().stream().map(this::normalizeField).toList();
        return new ProtocolConfigResponse(
                config.companyId(),
                config.brokerName(),
                config.topicPattern(),
                topicExample(config.topicPattern()),
                config.payloadRoot(),
                fields,
                rulesOrDefault(config.temperatureRules()),
                previewPayload(config.payloadRoot(), fields)
        );
    }

    private ProtocolFieldConfigResponse normalizeField(ProtocolFieldConfigResponse field) {
        String targetField = clean(field.targetField());
        String dataType = normalizeDataType(field.dataType(), targetField);
        String alertMode = normalizeAlertMode(field.alertMode(), targetField, dataType);
        return new ProtocolFieldConfigResponse(
                clean(field.key()),
                clean(field.label()),
                field.enabled(),
                clean(field.jsonPath()),
                dataType,
                cleanOptional(field.unit()),
                sampleValue(field.sampleValue(), targetField, dataType),
                targetField,
                field.required() == null ? defaultRequired(targetField) : field.required(),
                alertMode,
                alertActivationValue(field.alertActivationValue(), targetField, alertMode),
                alertMin(field.alertMin(), targetField, alertMode),
                alertMax(field.alertMax(), targetField, alertMode),
                alertIcon(field.alertIcon(), targetField)
        );
    }

    private TemperatureRulesResponse rulesFromFields(List<ProtocolFieldConfigResponse> fields, TemperatureRulesResponse requestRules) {
        TemperatureRulesResponse fallback = rulesOrDefault(requestRules);
        ProtocolFieldConfigResponse temperature = fieldForTarget(fields == null ? List.of() : fields, "temperature");
        if (temperature == null || !"RANGE".equalsIgnoreCase(clean(temperature.alertMode()))) {
            return fallback;
        }
        Double min = temperature.alertMin() == null ? fallback.minAllowed() : temperature.alertMin();
        Double max = temperature.alertMax() == null ? fallback.maxAllowed() : temperature.alertMax();
        return new TemperatureRulesResponse(min, max, min - 3.0, max + 3.0);
    }

    private String normalizeAlertMode(String alertMode, String targetField, String dataType) {
        String normalized = clean(alertMode).toUpperCase(java.util.Locale.ROOT);
        if ("NUMBER".equalsIgnoreCase(dataType)) {
            return normalized.equals("RANGE") ? "RANGE" : "NONE";
        }
        if ("BOOLEAN".equalsIgnoreCase(dataType)) {
            return normalized.equals("ACTIVATION") ? "ACTIVATION" : defaultBooleanAlertMode(targetField);
        }
        if (normalized.equals("ACTIVATION") || normalized.equals("NONE")) {
            return normalized;
        }
        return "NONE";
    }

    private String defaultBooleanAlertMode(String targetField) {
        return switch (clean(targetField)) {
            case "doorState", "coolingUnitState" -> "ACTIVATION";
            default -> "NONE";
        };
    }

    private String alertActivationValue(String value, String targetField, String alertMode) {
        if (!"ACTIVATION".equalsIgnoreCase(alertMode)) {
            return "";
        }
        String cleanValue = cleanOptional(value);
        if (!cleanValue.isBlank()) {
            return normalizeActivationValue(cleanValue, targetField);
        }
        return switch (clean(targetField)) {
            case "doorState" -> "true";
            case "coolingUnitState" -> "false";
            default -> "true";
        };
    }

    private String alertIcon(String value, String targetField) {
        String cleanValue = cleanOptional(value);
        if (!cleanValue.isBlank()) {
            return cleanValue;
        }
        return switch (clean(targetField)) {
            case "temperature" -> "fa-solid fa-temperature-half";
            case "humidity" -> "fa-solid fa-droplet";
            case "doorState" -> "fa-solid fa-door-open";
            case "coolingUnitState" -> "fa-regular fa-snowflake";
            case "fuelLevel" -> "fa-solid fa-gas-pump";
            case "speed" -> "fa-solid fa-gauge-high";
            case "battery" -> "fa-solid fa-battery-half";
            case "latitude", "longitude" -> "fa-solid fa-location-dot";
            default -> "fa-solid fa-triangle-exclamation";
        };
    }

    private String normalizeActivationValue(String value, String targetField) {
        String normalized = clean(value).toLowerCase(java.util.Locale.ROOT);
        if (normalized.equals("both") || normalized.equals("double") || normalized.equals("doble")) {
            return "BOTH";
        }
        if ("doorState".equalsIgnoreCase(clean(targetField))) {
            if (normalized.equals("abierta") || normalized.equals("open") || normalized.equals("1")) {
                return "true";
            }
            if (normalized.equals("cerrada") || normalized.equals("closed") || normalized.equals("0")) {
                return "false";
            }
        }
        if ("coolingUnitState".equalsIgnoreCase(clean(targetField))) {
            if (normalized.equals("encendido") || normalized.equals("on") || normalized.equals("1")) {
                return "true";
            }
            if (normalized.equals("apagado") || normalized.equals("off") || normalized.equals("0")) {
                return "false";
            }
        }
        if (normalized.equals("1")) {
            return "true";
        }
        if (normalized.equals("0")) {
            return "false";
        }
        return value;
    }

    private String sampleValue(String value, String targetField, String dataType) {
        String cleanValue = cleanOptional(value);
        if (!"BOOLEAN".equalsIgnoreCase(dataType)) {
            return cleanValue;
        }
        return normalizeActivationValue(cleanValue, targetField);
    }

    private Double alertMin(Double value, String targetField, String alertMode) {
        if (!"RANGE".equalsIgnoreCase(alertMode)) {
            return null;
        }
        if (value != null) {
            return value;
        }
        return "temperature".equalsIgnoreCase(clean(targetField)) ? defaultTemperatureRules().minAllowed() : 0.0;
    }

    private Double alertMax(Double value, String targetField, String alertMode) {
        if (!"RANGE".equalsIgnoreCase(alertMode)) {
            return null;
        }
        if (value != null) {
            return value;
        }
        return "temperature".equalsIgnoreCase(clean(targetField)) ? defaultTemperatureRules().maxAllowed() : 100.0;
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
            case "BOOLEAN" -> parseBoolean(value);
            default -> value;
        };
    }

    private Boolean parseBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("abierta") || normalized.equals("encendido");
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
                        new ProtocolFieldConfigResponse("temperature", "Temperatura", true, "temperatura", "NUMBER", "C", "4.8", "temperature", true, "RANGE", "", -2.0, 5.0, "fa-solid fa-temperature-half"),
                        new ProtocolFieldConfigResponse("humidity", "Humedad", true, "humedad", "NUMBER", "%", "45", "humidity", false, "NONE", "", null, null, "fa-solid fa-droplet"),
                        new ProtocolFieldConfigResponse("doorState", "Puerta", true, "puerta", "BOOLEAN", "", "false", "doorState", false, "ACTIVATION", "true", null, null, "fa-solid fa-door-open"),
                        new ProtocolFieldConfigResponse("coolingUnitState", "Equipo de frio", true, "equipoFrio", "BOOLEAN", "", "true", "coolingUnitState", false, "ACTIVATION", "false", null, null, "fa-regular fa-snowflake"),
                        new ProtocolFieldConfigResponse("fuelLevel", "Combustible", true, "combustible", "NUMBER", "%", "65", "fuelLevel", false, "NONE", "", null, null, "fa-solid fa-gas-pump"),
                        new ProtocolFieldConfigResponse("speed", "Velocidad", true, "velocidad", "NUMBER", "km/h", "65", "speed", false, "NONE", "", null, null, "fa-solid fa-gauge-high"),
                        new ProtocolFieldConfigResponse("latitude", "Latitud", true, "ubicacion.lat", "NUMBER", "", "-12.0576", "latitude", false, "NONE", "", null, null, "fa-solid fa-location-dot"),
                        new ProtocolFieldConfigResponse("longitude", "Longitud", true, "ubicacion.lng", "NUMBER", "", "-76.9649", "longitude", false, "NONE", "", null, null, "fa-solid fa-location-dot"),
                        new ProtocolFieldConfigResponse("battery", "Bateria", false, "bateria", "NUMBER", "%", "92", "battery", false, "NONE", "", null, null, "fa-solid fa-battery-half")
                ),
                defaultTemperatureRules()
        );
    }

    private void loadConfigs() {
        configs.putAll(jsonStoreService.read(
                "protocol-configs",
                storePath,
                new TypeReference<Map<Long, StoredProtocolConfig>>() {},
                this::defaultConfigs,
                "No se pudo cargar configuraciones de protocolo",
                "No se pudo persistir configuracion de protocolo"
        ));
    }



    private void saveConfigs() {
        jsonStoreService.write("protocol-configs", storePath, configs, "No se pudo persistir configuracion de protocolo");
    }

    private Map<Long, StoredProtocolConfig> defaultConfigs() {
        Map<Long, StoredProtocolConfig> defaults = new LinkedHashMap<>();
        defaults.put(1L, defaultConfig(1L));
        defaults.put(2L, defaultConfig(2L));
        return defaults;
    }

    private boolean defaultRequired(String targetField) {
        return "temperature".equalsIgnoreCase(clean(targetField));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeDataType(String dataType) {
        return normalizeDataType(dataType, "");
    }

    private String normalizeDataType(String dataType, String targetField) {
        if ("doorState".equalsIgnoreCase(clean(targetField)) || "coolingUnitState".equalsIgnoreCase(clean(targetField))) {
            return "BOOLEAN";
        }
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
            List<ProtocolFieldConfigResponse> fields,
            TemperatureRulesResponse temperatureRules
    ) {
    }
}
