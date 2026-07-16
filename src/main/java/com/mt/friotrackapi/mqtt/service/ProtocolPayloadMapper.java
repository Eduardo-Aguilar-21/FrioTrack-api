package com.mt.friotrackapi.mqtt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.mqtt.dto.ProtocolTelemetryData;
import com.mt.friotrackapi.protocol.dto.ProtocolConfigResponse;
import com.mt.friotrackapi.protocol.dto.ProtocolFieldConfigResponse;
import com.mt.friotrackapi.protocol.dto.TemperatureRulesResponse;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class ProtocolPayloadMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProtocolConfigService protocolConfigService;

    public ProtocolPayloadMapper(ProtocolConfigService protocolConfigService) {
        this.protocolConfigService = protocolConfigService;
    }

    public ProtocolTelemetryData map(Long companyId, String payload) {
        ProtocolConfigResponse config = protocolConfigService.findByCompany(companyId);
        List<String> errors = new ArrayList<>();

        String temperature = null;
        Double temperatureValue = null;
        String temperatureState = null;
        String humidity = null;
        String doorState = null;
        String coolingUnitState = null;
        String fuelLevel = null;
        String speed = null;
        Double latitude = null;
        Double longitude = null;
        Map<String, Object> customFields = new LinkedHashMap<>();

        TemperatureRulesResponse rules = config.temperatureRules();

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode source = sourceRoot(root, config.payloadRoot(), errors);

            for (ProtocolFieldConfigResponse field : config.fields()) {
                if (!field.enabled() || isBlank(field.jsonPath()) || isBlank(field.targetField())) {
                    continue;
                }

                JsonNode node = readPath(source, field.jsonPath());
                if (node == null || node.isMissingNode() || node.isNull()) {
                    if (Boolean.TRUE.equals(field.required())) {
                        errors.add("Campo requerido no encontrado: " + field.jsonPath());
                    }
                    continue;
                }

                try {
                    switch (field.targetField()) {
                        case "temperature" -> {
                            temperatureValue = asDouble(node, field);
                            temperature = formatNumber(temperatureValue) + " °C";
                            temperatureState = temperatureState(temperatureValue, rules);
                        }
                        case "humidity" -> humidity = formatWithUnit(asDouble(node, field), field.unit());
                        case "doorState" -> doorState = doorState(node, field);
                        case "coolingUnitState" -> coolingUnitState = coolingUnitState(node, field);
                        case "fuelLevel" -> fuelLevel = formatWithUnit(asDouble(node, field), field.unit());
                        case "speed" -> speed = formatWithUnit(asDouble(node, field), field.unit());
                        case "latitude" -> latitude = asDouble(node, field);
                        case "longitude" -> longitude = asDouble(node, field);
                        default -> customFields.put(field.targetField(), valueForCustomField(node, field));
                    }
                } catch (IllegalArgumentException ex) {
                    errors.add(ex.getMessage());
                }
            }
        } catch (Exception ex) {
            errors.add("JSON invalido: " + ex.getMessage());
        }

        return new ProtocolTelemetryData(temperature, temperatureValue, temperatureState, humidity, doorState, coolingUnitState, fuelLevel, speed, latitude, longitude, customFields, errors);
    }

    private JsonNode sourceRoot(JsonNode root, String payloadRoot, List<String> errors) {
        if (isBlank(payloadRoot)) {
            return root;
        }

        JsonNode node = readPath(root, payloadRoot);
        if (node == null || node.isMissingNode() || node.isNull()) {
            errors.add("Raiz de payload no encontrada: " + payloadRoot);
            return root;
        }
        return node;
    }

    private JsonNode readPath(JsonNode root, String path) {
        JsonNode current = root;
        for (String part : path.split("\\.")) {
            if (isBlank(part)) {
                continue;
            }
            current = current.path(part.trim());
            if (current.isMissingNode()) {
                return current;
            }
        }
        return current;
    }

    private String doorState(JsonNode node, ProtocolFieldConfigResponse field) {
        if ("BOOLEAN".equalsIgnoreCase(field.dataType())) {
            return asBoolean(node) ? "Abierta" : "Cerrada";
        }
        return asText(node);
    }

    private String coolingUnitState(JsonNode node, ProtocolFieldConfigResponse field) {
        if ("BOOLEAN".equalsIgnoreCase(field.dataType())) {
            return asBoolean(node) ? "Encendido" : "Apagado";
        }
        return asText(node);
    }

    private boolean asBoolean(JsonNode node) {
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.asInt() != 0;
        }
        String normalized = asText(node).toLowerCase(Locale.US);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("abierta") || normalized.equals("encendido");
    }

    private Object valueForCustomField(JsonNode node, ProtocolFieldConfigResponse field) {
        return switch (normalizedDataType(field.dataType())) {
            case "NUMBER" -> asDouble(node, field);
            case "BOOLEAN" -> asBoolean(node);
            default -> asText(node);
        };
    }

    private String normalizedDataType(String dataType) {
        if (dataType == null || dataType.isBlank()) {
            return "STRING";
        }
        String normalized = dataType.trim().toUpperCase(Locale.US);
        return switch (normalized) {
            case "NUMBER", "BOOLEAN", "STRING" -> normalized;
            default -> "STRING";
        };
    }

    private Double asDouble(JsonNode node, ProtocolFieldConfigResponse field) {
        if (node.isNumber()) {
            return node.asDouble();
        }

        String text = asText(node).replace("°C", "").replace("C", "").replace("%", "").replace("km/h", "").trim();
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Campo " + field.jsonPath() + " no es numerico");
        }
    }

    private String asText(JsonNode node) {
        if (node.isTextual()) {
            return node.asText().trim();
        }
        if (node.isBoolean()) {
            return Boolean.toString(node.asBoolean());
        }
        if (node.isNumber()) {
            return formatNumber(node.asDouble());
        }
        return node.toString();
    }

    private String formatWithUnit(Double value, String unit) {
        String normalizedUnit = unit == null ? "" : unit.trim();
        return normalizedUnit.isBlank() ? formatNumber(value) : formatNumber(value) + " " + normalizedUnit;
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "--";
        }
        if (Math.floor(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String temperatureState(Double value, TemperatureRulesResponse rules) {
        if (value == null) {
            return "Sin datos";
        }
        return value >= rules.minAllowed() && value <= rules.maxAllowed() ? "En rango" : "Fuera de rango";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
