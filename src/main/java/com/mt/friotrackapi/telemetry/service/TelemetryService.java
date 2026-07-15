package com.mt.friotrackapi.telemetry.service;

import com.mt.friotrackapi.telemetry.dto.TelemetrySnapshotResponse;
import com.mt.friotrackapi.telemetry.dto.TemperaturePointResponse;
import com.mt.friotrackapi.telemetry.dto.VehicleEventResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TelemetryService {

    public TelemetrySnapshotResponse snapshot(Long vehicleId) {
        return new TelemetrySnapshotResponse(
                vehicleId,
                "9.8 °C",
                "Fuera de rango",
                "45 %",
                "Abierta",
                "Encendido",
                "65 %",
                "65 km/h",
                "-2 °C a 5 °C",
                -12.0576,
                -76.9649,
                "Av. Nicolas Ayllon 3980, Ate, Lima",
                "Hace 1 min"
        );
    }

    public List<TemperaturePointResponse> temperatureHistory(Long vehicleId) {
        return List.of(
                new TemperaturePointResponse("12:00", 2.4),
                new TemperaturePointResponse("14:00", 0.3),
                new TemperaturePointResponse("16:00", -1.4),
                new TemperaturePointResponse("18:00", -0.7),
                new TemperaturePointResponse("20:00", -0.8),
                new TemperaturePointResponse("22:00", -1.5),
                new TemperaturePointResponse("00:00", -2.2),
                new TemperaturePointResponse("02:00", -0.4),
                new TemperaturePointResponse("04:00", 0.8),
                new TemperaturePointResponse("06:00", 3.2),
                new TemperaturePointResponse("08:00", 7.4),
                new TemperaturePointResponse("10:00", 10.4),
                new TemperaturePointResponse("12:00", 12.2)
        );
    }

    public List<VehicleEventResponse> events(Long vehicleId) {
        return List.of(
                new VehicleEventResponse("TEMPERATURE", "Temperatura alta: 9.8 °C", "Fuera de rango", "10:32", "CRITICAL"),
                new VehicleEventResponse("DOOR", "Puerta abierta", "Puerta delantera", "10:30", "WARNING"),
                new VehicleEventResponse("COOLING", "Equipo de frio encendido", "Compresor activado", "10:28", "INFO"),
                new VehicleEventResponse("NETWORK", "Comunicacion restablecida", "Senal OK", "10:27", "INFO")
        );
    }
}
