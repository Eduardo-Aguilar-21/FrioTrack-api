package com.mt.friotrackapi.mqtt.service;

import com.mt.friotrackapi.alerts.service.AlertService;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.mqtt.dto.ProtocolTelemetryData;
import com.mt.friotrackapi.protocol.dto.ProtocolFieldConfigResponse;
import com.mt.friotrackapi.protocol.dto.TemperatureRulesResponse;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import com.mt.friotrackapi.telemetry.service.TelemetryService;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import com.mt.friotrackapi.vehicles.service.VehicleService;
import org.springframework.stereotype.Service;

@Service
public class MqttTelemetryIngestionService {

    private final VehicleService vehicleService;
    private final ProtocolPayloadMapper protocolPayloadMapper;
    private final TelemetryService telemetryService;
    private final AlertService alertService;
    private final ProtocolConfigService protocolConfigService;

    public MqttTelemetryIngestionService(
            VehicleService vehicleService,
            ProtocolPayloadMapper protocolPayloadMapper,
            TelemetryService telemetryService,
            AlertService alertService,
            ProtocolConfigService protocolConfigService
    ) {
        this.vehicleService = vehicleService;
        this.protocolPayloadMapper = protocolPayloadMapper;
        this.telemetryService = telemetryService;
        this.alertService = alertService;
        this.protocolConfigService = protocolConfigService;
    }

    public void ingest(String topic, String payload) {
        Long vehicleId = vehicleIdFromTopic(topic);
        VehicleResponse vehicle = vehicleService.findById(vehicleId);
        validateTopicPattern(protocolConfigService.findByCompany(vehicle.companyId()).topicPattern(), topic, vehicleId);
        ProtocolTelemetryData data = protocolPayloadMapper.map(vehicle.companyId(), payload);

        telemetryService.applyMqttTelemetry(vehicle, data);
        VehicleResponse updatedVehicle = vehicleService.updateTelemetryState(
                vehicle.id(),
                data.latitude(),
                data.longitude(),
                data.temperature(),
                data.temperatureState(),
                data.doorState(),
                data.coolingUnitState(),
                "Ahora"
        );

        alertService.resolveMqttAlert(updatedVehicle.companyId(), "NETWORK", updatedVehicle.code());
        alertService.resolveMqttAlert(updatedVehicle.companyId(), "NETWORK_WARNING", updatedVehicle.code());

        processTemperature(updatedVehicle, data);
        processDoor(updatedVehicle, data);
        processCooling(updatedVehicle, data);
        processCustomFieldAlerts(updatedVehicle, data);
        processMapperErrors(updatedVehicle, data);
    }

    private Long vehicleIdFromTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new ApiException("Topic MQTT vacio");
        }

        for (String part : topic.split("/")) {
            try {
                return Long.parseLong(part);
            } catch (NumberFormatException ignored) {
                // Keep scanning the topic. The configured pattern is validated after the vehicle is identified.
            }
        }

        throw new ApiException("Id de vehiculo invalido en topic MQTT: " + topic);
    }

    private void validateTopicPattern(String topicPattern, String topic, Long vehicleId) {
        String pattern = topicPattern == null || topicPattern.isBlank() ? "vehiculo/{id}" : topicPattern.trim();
        String expected = pattern.replace("{id}", String.valueOf(vehicleId));
        if (!expected.equals(topic)) {
            throw new ApiException("Topic MQTT no coincide con el patron configurado: " + topic);
        }
    }

    private void processTemperature(VehicleResponse vehicle, ProtocolTelemetryData data) {
        ProtocolFieldConfigResponse field = protocolConfigService.fieldForTarget(vehicle.companyId(), "temperature");
        if (data.temperatureValue() == null || field == null || !"RANGE".equalsIgnoreCase(field.alertMode())) {
            alertService.resolveMqttAlert(vehicle.companyId(), "TEMPERATURE", vehicle.code());
            return;
        }

        double value = data.temperatureValue();
        double min = field.alertMin() == null ? protocolConfigService.temperatureRules(vehicle.companyId()).minAllowed() : field.alertMin();
        double max = field.alertMax() == null ? protocolConfigService.temperatureRules(vehicle.companyId()).maxAllowed() : field.alertMax();
        TemperatureRulesResponse rules = protocolConfigService.temperatureRules(vehicle.companyId());
        if (value > max || value < min) {
            String severity = value > rules.criticalHigh() || value < rules.criticalLow() ? "CRITICAL" : "WARNING";
            String title = value > max ? "Temperatura alta" : "Temperatura baja";
            String description = value > max
                    ? "Temperatura recibida por MQTT: " + data.temperature()
                    : "Temperatura recibida por MQTT debajo del limite: " + data.temperature();
            alertService.recordMqttAlert(vehicle.companyId(), "TEMPERATURE", severity, title, description, vehicle.label(), vehicle.code(), field.alertIcon(), data.temperature());
            telemetryService.recordMqttEvent(vehicle.id(), "TEMPERATURE", title + ": " + data.temperature(), "Fuera de rango", severity);
            return;
        }

        alertService.resolveMqttAlert(vehicle.companyId(), "TEMPERATURE", vehicle.code());
    }

    private void processDoor(VehicleResponse vehicle, ProtocolTelemetryData data) {
        ProtocolFieldConfigResponse field = protocolConfigService.fieldForTarget(vehicle.companyId(), "doorState");
        if (data.doorState() == null || field == null || !"ACTIVATION".equalsIgnoreCase(field.alertMode())) {
            alertService.resolveMqttAlert(vehicle.companyId(), "DOOR", vehicle.code());
            return;
        }

        if (matchesActivation(data.doorState(), field.alertActivationValue())) {
            String title = isDoubleActivation(field.alertActivationValue()) ? "Puerta: " + data.doorState() : "Puerta abierta";
            alertService.recordMqttAlert(vehicle.companyId(), "DOOR", "WARNING", title, "Estado recibido por MQTT: " + data.doorState(), vehicle.label(), vehicle.code(), field.alertIcon(), data.doorState());
            telemetryService.recordMqttEvent(vehicle.id(), "DOOR", title, "Estado recibido por MQTT", "WARNING");
            return;
        }

        alertService.resolveMqttAlert(vehicle.companyId(), "DOOR", vehicle.code());
    }

    private void processCooling(VehicleResponse vehicle, ProtocolTelemetryData data) {
        ProtocolFieldConfigResponse field = protocolConfigService.fieldForTarget(vehicle.companyId(), "coolingUnitState");
        if (data.coolingUnitState() == null || field == null || !"ACTIVATION".equalsIgnoreCase(field.alertMode())) {
            alertService.resolveMqttAlert(vehicle.companyId(), "COOLING", vehicle.code());
            return;
        }

        if (matchesActivation(data.coolingUnitState(), field.alertActivationValue())) {
            String title = isDoubleActivation(field.alertActivationValue()) ? "Equipo de frio: " + data.coolingUnitState() : "Equipo de frio apagado";
            alertService.recordMqttAlert(vehicle.companyId(), "COOLING", "CRITICAL", title, "Estado recibido por MQTT: " + data.coolingUnitState(), vehicle.label(), vehicle.code(), field.alertIcon(), data.coolingUnitState());
            telemetryService.recordMqttEvent(vehicle.id(), "COOLING", title, "Estado recibido por MQTT", "CRITICAL");
            return;
        }

        alertService.resolveMqttAlert(vehicle.companyId(), "COOLING", vehicle.code());
    }

    private void processCustomFieldAlerts(VehicleResponse vehicle, ProtocolTelemetryData data) {
        if (data.customFields() == null || data.customFields().isEmpty()) {
            return;
        }

        for (var entry : data.customFields().entrySet()) {
            ProtocolFieldConfigResponse field = protocolConfigService.fieldForTarget(vehicle.companyId(), entry.getKey());
            if (field == null || "NONE".equalsIgnoreCase(field.alertMode())) {
                continue;
            }
            String type = "CUSTOM_" + entry.getKey().toUpperCase(java.util.Locale.ROOT);
            if (isTriggered(field, entry.getValue())) {
                String title = field.label() + " fuera de condicion";
                String description = "Valor recibido por MQTT: " + entry.getValue();
                alertService.recordMqttAlert(vehicle.companyId(), type, "WARNING", title, description, vehicle.label(), vehicle.code(), field.alertIcon(), String.valueOf(entry.getValue()));
                telemetryService.recordMqttEvent(vehicle.id(), type, title, description, "WARNING");
            } else {
                alertService.resolveMqttAlert(vehicle.companyId(), type, vehicle.code());
            }
        }
    }

    private boolean isTriggered(ProtocolFieldConfigResponse field, Object value) {
        if ("ACTIVATION".equalsIgnoreCase(field.alertMode())) {
            return matchesActivation(String.valueOf(value), field.alertActivationValue());
        }
        if ("RANGE".equalsIgnoreCase(field.alertMode())) {
            Double number = asDouble(value);
            if (number == null) {
                return false;
            }
            Double min = field.alertMin();
            Double max = field.alertMax();
            return (min != null && number < min) || (max != null && number > max);
        }
        return false;
    }

    private boolean matchesActivation(String value, String expected) {
        if (isDoubleActivation(expected)) {
            return value != null && !value.trim().isBlank();
        }
        String actual = value == null ? "" : value.trim();
        String target = expected == null || expected.isBlank() ? "true" : expected.trim();
        return actual.equalsIgnoreCase(target) || actual.toLowerCase(java.util.Locale.ROOT).contains(target.toLowerCase(java.util.Locale.ROOT));
    }

    private boolean isDoubleActivation(String expected) {
        return expected != null && expected.trim().equalsIgnoreCase("BOTH");
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).replace("%", "").replace("°C", "").replace("C", "").trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private void processMapperErrors(VehicleResponse vehicle, ProtocolTelemetryData data) {
        if (data.errors() == null || data.errors().isEmpty()) {
            alertService.resolveMqttAlert(vehicle.companyId(), "SENSOR", vehicle.code());
            return;
        }

        String description = String.join("; ", data.errors());
        alertService.recordMqttAlert(vehicle.companyId(), "SENSOR", "INFO", "Payload MQTT con observaciones", description, vehicle.label(), vehicle.code(), null, description);
    }
}
