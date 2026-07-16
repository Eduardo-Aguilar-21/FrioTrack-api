package com.mt.friotrackapi.mqtt.service;

import com.mt.friotrackapi.protocol.dto.ProtocolConfigResponse;
import com.mt.friotrackapi.protocol.dto.ProtocolFieldConfigResponse;
import com.mt.friotrackapi.protocol.dto.TemperatureRulesResponse;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProtocolPayloadMapperTest {

    @Test
    void mapsCustomFieldsAndIgnoresMissingOptionalFields() {
        ProtocolConfigService configService = mock(ProtocolConfigService.class);
        when(configService.findByCompany(1L)).thenReturn(new ProtocolConfigResponse(
                1L,
                "Broker",
                "vehiculo/{id}",
                "vehiculo/12",
                "",
                List.of(
                        new ProtocolFieldConfigResponse("temperature", "Temperatura", true, "temperatura", "NUMBER", "C", "4.8", "temperature", true),
                        new ProtocolFieldConfigResponse("humidity", "Humedad", true, "humedad", "NUMBER", "%", "45", "humidity", false),
                        new ProtocolFieldConfigResponse("battery", "Bateria", true, "bateria", "NUMBER", "%", "88", "battery", false)
                ),
                new TemperatureRulesResponse(-2.0, 5.0, -5.0, 8.0),
                Map.of()
        ));

        ProtocolPayloadMapper mapper = new ProtocolPayloadMapper(configService);
        var data = mapper.map(1L, "{\"temperatura\":4.2,\"bateria\":88}");

        assertEquals("4.2 °C", data.temperature());
        assertEquals("En rango", data.temperatureState());
        assertEquals(88.0, data.customFields().get("battery"));
        assertTrue(data.errors().isEmpty());
    }
}
