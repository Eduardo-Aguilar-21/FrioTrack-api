package com.mt.friotrackapi.mqtt.service;

import com.mt.friotrackapi.alerts.service.AlertService;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.mqtt.dto.ProtocolTelemetryData;
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

        processTemperature(updatedVehicle, data);
        processDoor(updatedVehicle, data);
        processCooling(updatedVehicle, data);
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
        if (data.temperatureValue() == null) {
            return;
        }

        double value = data.temperatureValue();
        TemperatureRulesResponse rules = protocolConfigService.temperatureRules(vehicle.companyId());
        if (value > rules.maxAllowed() || value < rules.minAllowed()) {
            String severity = value > rules.criticalHigh() || value < rules.criticalLow() ? "CRITICAL" : "WARNING";
            String title = value > rules.maxAllowed() ? "Temperatura alta" : "Temperatura baja";
            String description = value > rules.maxAllowed()
                    ? "Temperatura recibida por MQTT: " + data.temperature()
                    : "Temperatura recibida por MQTT debajo del limite: " + data.temperature();
            alertService.recordMqttAlert(vehicle.companyId(), "TEMPERATURE", severity, title, description, vehicle.label(), vehicle.code());
            telemetryService.recordMqttEvent(vehicle.id(), "TEMPERATURE", title + ": " + data.temperature(), "Fuera de rango", severity);
            return;
        }

        alertService.resolveMqttAlert(vehicle.companyId(), "TEMPERATURE", vehicle.code());
    }

    private void processDoor(VehicleResponse vehicle, ProtocolTelemetryData data) {
        if (data.doorState() == null) {
            return;
        }

        if (data.doorState().toLowerCase().contains("abierta")) {
            alertService.recordMqttAlert(vehicle.companyId(), "DOOR", "WARNING", "Puerta abierta", "Estado recibido por MQTT: " + data.doorState(), vehicle.label(), vehicle.code());
            telemetryService.recordMqttEvent(vehicle.id(), "DOOR", "Puerta abierta", "Estado recibido por MQTT", "WARNING");
            return;
        }

        alertService.resolveMqttAlert(vehicle.companyId(), "DOOR", vehicle.code());
    }

    private void processCooling(VehicleResponse vehicle, ProtocolTelemetryData data) {
        if (data.coolingUnitState() == null) {
            return;
        }

        String state = data.coolingUnitState().toLowerCase();
        if (state.contains("apag") || state.contains("off")) {
            alertService.recordMqttAlert(vehicle.companyId(), "COOLING", "CRITICAL", "Equipo de frio apagado", "Estado recibido por MQTT: " + data.coolingUnitState(), vehicle.label(), vehicle.code());
            telemetryService.recordMqttEvent(vehicle.id(), "COOLING", "Equipo de frio apagado", "Estado recibido por MQTT", "CRITICAL");
            return;
        }

        alertService.resolveMqttAlert(vehicle.companyId(), "COOLING", vehicle.code());
    }

    private void processMapperErrors(VehicleResponse vehicle, ProtocolTelemetryData data) {
        if (data.errors() == null || data.errors().isEmpty()) {
            alertService.resolveMqttAlert(vehicle.companyId(), "SENSOR", vehicle.code());
            return;
        }

        String description = String.join("; ", data.errors());
        alertService.recordMqttAlert(vehicle.companyId(), "SENSOR", "INFO", "Payload MQTT con observaciones", description, vehicle.label(), vehicle.code());
    }
}
