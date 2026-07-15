package com.mt.friotrackapi.vehicles.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.vehicles.dto.VehicleResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final CompanyService companyService;
    private final List<VehicleResponse> vehicles = List.of(
            new VehicleResponse(1L, 1L, "AAA111", "AAA-111", "Camion 01 - AAA111", "EN_RANGO", "Carlos Ruiz", "865612040015601", "Hino 500", 2022, "Refrigerado", 12000, -11.985, -77.065, "2.1 °C", "En rango", "Cerrada", "Encendido", "Hace 1 min"),
            new VehicleResponse(2L, 1L, "BBB222", "BBB-222", "Camion 02 - BBB222", "EN_RANGO", "Luis Mendoza", "865612040015602", "Hino 500", 2021, "Refrigerado", 12000, -12.010, -77.115, "3.4 °C", "En rango", "Cerrada", "Encendido", "Hace 2 min"),
            new VehicleResponse(3L, 1L, "GHI789", "GHI-789", "Camion 03 - GHI789", "ADVERTENCIA", "Marco Salas", "865612040015603", "Isuzu NPR", 2020, "Refrigerado", 9000, -12.027, -77.014, "6.7 °C", "Fuera de rango", "Cerrada", "Encendido", "Hace 2 min"),
            new VehicleResponse(7L, 1L, "DEF456", "DEF-456", "Camion 07 - DEF456", "ADVERTENCIA", "Rafael Torres", "865612040015607", "Hino 500", 2022, "Refrigerado", 12000, -12.071, -76.995, "4.8 °C", "En rango", "Abierta", "Encendido", "Hace 3 min"),
            new VehicleResponse(12L, 1L, "ABC123", "ABC-123", "Camion 12 - ABC123", "CRITICO", "Juan Perez", "865612040015678", "Hino 500", 2022, "Refrigerado", 12000, -12.0576, -76.9649, "9.8 °C", "Fuera de rango", "Cerrada", "Encendido", "Hace 1 min"),
            new VehicleResponse(21L, 1L, "MNO321", "MNO-321", "Camion 21 - MNO321", "SIN_COMUNICACION", "Pedro Vargas", "865612040015621", "Foton Aumark", 2019, "Refrigerado", 8000, -12.115, -77.035, null, "Sin datos", "--", "--", "Hace 25 min"),
            new VehicleResponse(101L, 2L, "NOR111", "NOR-111", "Camion Norte 01 - NOR111", "ADVERTENCIA", "Ana Castro", "865612040015701", "Hino 500", 2022, "Refrigerado", 12000, -8.1116, -79.0288, "6.2 °C", "Fuera de rango", "Cerrada", "Encendido", "Hace 4 min")
    );

    public VehicleService(CompanyService companyService) {
        this.companyService = companyService;
    }

    public List<VehicleResponse> findAll(Long companyId) {
        if (companyId == null) {
            return vehicles;
        }

        companyService.findById(companyId);
        return vehicles.stream()
                .filter(vehicle -> vehicle.companyId().equals(companyId))
                .toList();
    }

    public VehicleResponse findById(Long id) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException("Vehiculo no encontrado"));
    }
}
