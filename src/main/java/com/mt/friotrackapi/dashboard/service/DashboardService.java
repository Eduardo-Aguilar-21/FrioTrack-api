package com.mt.friotrackapi.dashboard.service;

import com.mt.friotrackapi.dashboard.dto.DashboardSummaryResponse;
import com.mt.friotrackapi.dashboard.dto.FleetMapVehicleResponse;
import com.mt.friotrackapi.dashboard.dto.TemperatureDistributionResponse;
import com.mt.friotrackapi.dashboard.dto.VehicleStatusResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    public DashboardSummaryResponse summary(Long companyId) {
        return new DashboardSummaryResponse(25, 18, 4, 2, 1, "2.7 °C");
    }

    public List<FleetMapVehicleResponse> fleetMap(Long companyId) {
        return List.of(
                new FleetMapVehicleResponse(1L, "Camion 01 - AAA111", -11.985, -77.065, "EN_RANGO", "#35b553", "2.1 °C"),
                new FleetMapVehicleResponse(2L, "Camion 02 - BBB222", -12.010, -77.115, "EN_RANGO", "#35b553", "3.4 °C"),
                new FleetMapVehicleResponse(3L, "Camion 03 - GHI789", -12.027, -77.014, "ADVERTENCIA", "#ffad0a", "6.7 °C"),
                new FleetMapVehicleResponse(4L, "Camion 04 - CCC333", -12.005, -76.955, "EN_RANGO", "#35b553", "3.0 °C"),
                new FleetMapVehicleResponse(5L, "Camion 05 - DDD444", -12.080, -77.075, "EN_RANGO", "#35b553", "2.8 °C"),
                new FleetMapVehicleResponse(6L, "Camion 06 - EEE555", -12.071, -76.995, "EN_RANGO", "#35b553", "4.1 °C"),
                new FleetMapVehicleResponse(12L, "Camion 12 - ABC123", -12.055, -76.935, "CRITICO", "#ff2d2d", "9.8 °C"),
                new FleetMapVehicleResponse(21L, "Camion 21 - MNO321", -12.115, -77.035, "SIN_DATOS", "#70809b", "Sin datos")
        );
    }

    public List<VehicleStatusResponse> vehicleStatus(Long companyId) {
        return List.of(
                new VehicleStatusResponse(1L, "Camion 01 - AAA111", "En rango", "2.1 °C", "En rango", "Cerrada", "Encendido", "Hace 1 min"),
                new VehicleStatusResponse(2L, "Camion 02 - BBB222", "En rango", "3.4 °C", "En rango", "Cerrada", "Encendido", "Hace 2 min"),
                new VehicleStatusResponse(3L, "Camion 03 - GHI789", "Advertencia", "6.7 °C", "Fuera de rango", "Cerrada", "Encendido", "Hace 2 min"),
                new VehicleStatusResponse(7L, "Camion 07 - DEF456", "Advertencia", "4.8 °C", "En rango", "Abierta", "Encendido", "Hace 3 min"),
                new VehicleStatusResponse(12L, "Camion 12 - ABC123", "Critico", "9.8 °C", "Fuera de rango", "Cerrada", "Encendido", "Hace 1 min"),
                new VehicleStatusResponse(21L, "Camion 21 - MNO321", "Sin comunicacion", "--", "--", "--", "--", "Hace 25 min")
        );
    }

    public TemperatureDistributionResponse temperatureDistribution(Long companyId) {
        return new TemperatureDistributionResponse(18, 4, 2, 1, "-2 °C a 5 °C");
    }
}
