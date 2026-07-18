-- Demo telemetry payloads used by vehicle detail and tracking screens.

INSERT INTO app_store (store_key, payload, updated_at)
VALUES ('telemetry-snapshots', $json_telemetry_snapshots$
{
  "1": {
    "vehicleId": 1,
    "temperature": "2.1 °C",
    "temperatureState": "En rango",
    "humidity": "48 %",
    "doorState": "Cerrada",
    "coolingUnitState": "Encendido",
    "fuelLevel": "78 %",
    "speed": "58 km/h",
    "targetRange": "-2 °C a 5 °C",
    "latitude": -11.985,
    "longitude": -77.065,
    "address": "Panamericana Norte, Los Olivos",
    "lastCommunication": "Hace 1 min",
    "customFields": {}
  },
  "2": {
    "vehicleId": 2,
    "temperature": "3.4 °C",
    "temperatureState": "En rango",
    "humidity": "51 %",
    "doorState": "Cerrada",
    "coolingUnitState": "Encendido",
    "fuelLevel": "72 %",
    "speed": "54 km/h",
    "targetRange": "-2 °C a 5 °C",
    "latitude": -12.01,
    "longitude": -77.115,
    "address": "Av. Argentina, Callao",
    "lastCommunication": "Hace 2 min",
    "customFields": {}
  },
  "3": {
    "vehicleId": 3,
    "temperature": "6.7 °C",
    "temperatureState": "Fuera de rango",
    "humidity": "57 %",
    "doorState": "Cerrada",
    "coolingUnitState": "Encendido",
    "fuelLevel": "61 %",
    "speed": "49 km/h",
    "targetRange": "-2 °C a 5 °C",
    "latitude": -12.027,
    "longitude": -77.014,
    "address": "Av. Abancay, Lima",
    "lastCommunication": "Hace 2 min",
    "customFields": {}
  },
  "7": {
    "vehicleId": 7,
    "temperature": "4.8 °C",
    "temperatureState": "En rango",
    "humidity": "53 %",
    "doorState": "Abierta",
    "coolingUnitState": "Encendido",
    "fuelLevel": "66 %",
    "speed": "35 km/h",
    "targetRange": "-2 °C a 5 °C",
    "latitude": -12.071,
    "longitude": -76.995,
    "address": "Av. Javier Prado Este, San Borja",
    "lastCommunication": "Hace 3 min",
    "customFields": {}
  },
  "12": {
    "vehicleId": 12,
    "temperature": "4.8 °C",
    "temperatureState": "En rango",
    "humidity": "45 %",
    "doorState": "Cerrada",
    "coolingUnitState": "Encendido",
    "fuelLevel": "65 %",
    "speed": "65 km/h",
    "targetRange": "-2 °C a 5 °C",
    "latitude": -12.0576,
    "longitude": -76.9649,
    "address": "Ubicacion MQTT",
    "lastCommunication": "Ahora",
    "customFields": {}
  },
  "21": {
    "vehicleId": 21,
    "temperature": null,
    "temperatureState": "Sin datos",
    "humidity": null,
    "doorState": null,
    "coolingUnitState": null,
    "fuelLevel": null,
    "speed": null,
    "targetRange": "-2 °C a 5 °C",
    "latitude": -12.115,
    "longitude": -77.035,
    "address": "Ultima ubicacion: Chorrillos",
    "lastCommunication": "Hace 25 min",
    "customFields": {}
  },
  "101": {
    "vehicleId": 101,
    "temperature": "6.2 °C",
    "temperatureState": "Fuera de rango",
    "humidity": "55 %",
    "doorState": "Cerrada",
    "coolingUnitState": "Encendido",
    "fuelLevel": "70 %",
    "speed": "46 km/h",
    "targetRange": "-2 °C a 5 °C",
    "latitude": -8.1116,
    "longitude": -79.0288,
    "address": "Av. Espana, Trujillo",
    "lastCommunication": "Hace 4 min",
    "customFields": {}
  }
}
$json_telemetry_snapshots$, now())
ON CONFLICT (store_key) DO UPDATE
SET payload = EXCLUDED.payload, updated_at = now();

INSERT INTO app_store (store_key, payload, updated_at)
VALUES ('temperature-history', $json_temperature_history$
{
  "1": [
    {
      "time": "06:00",
      "temperature": 1.9
    },
    {
      "time": "07:00",
      "temperature": 2.0
    },
    {
      "time": "08:00",
      "temperature": 2.2
    },
    {
      "time": "09:00",
      "temperature": 2.4
    },
    {
      "time": "10:00",
      "temperature": 2.1
    },
    {
      "time": "11:00",
      "temperature": 2.3
    },
    {
      "time": "12:00",
      "temperature": 2.0
    },
    {
      "time": "13:00",
      "temperature": 1.8
    },
    {
      "time": "14:00",
      "temperature": 2.5
    },
    {
      "time": "15:00",
      "temperature": 2.7
    },
    {
      "time": "16:00",
      "temperature": 2.3
    },
    {
      "time": "17:00",
      "temperature": 2.1
    }
  ],
  "2": [
    {
      "time": "06:00",
      "temperature": 2.8
    },
    {
      "time": "07:00",
      "temperature": 3.1
    },
    {
      "time": "08:00",
      "temperature": 3.4
    },
    {
      "time": "09:00",
      "temperature": 3.7
    },
    {
      "time": "10:00",
      "temperature": 4.1
    },
    {
      "time": "11:00",
      "temperature": 4.4
    },
    {
      "time": "12:00",
      "temperature": 4.0
    },
    {
      "time": "13:00",
      "temperature": 3.6
    },
    {
      "time": "14:00",
      "temperature": 3.3
    },
    {
      "time": "15:00",
      "temperature": 3.5
    },
    {
      "time": "16:00",
      "temperature": 3.2
    },
    {
      "time": "17:00",
      "temperature": 3.4
    }
  ],
  "3": [
    {
      "time": "06:00",
      "temperature": 3.8
    },
    {
      "time": "07:00",
      "temperature": 4.2
    },
    {
      "time": "08:00",
      "temperature": 5.4
    },
    {
      "time": "09:00",
      "temperature": 6.1
    },
    {
      "time": "10:00",
      "temperature": 6.7
    },
    {
      "time": "11:00",
      "temperature": 7.2
    },
    {
      "time": "12:00",
      "temperature": 6.9
    },
    {
      "time": "13:00",
      "temperature": 5.8
    },
    {
      "time": "14:00",
      "temperature": 4.9
    },
    {
      "time": "15:00",
      "temperature": 4.4
    },
    {
      "time": "16:00",
      "temperature": 5.6
    },
    {
      "time": "17:00",
      "temperature": 6.7
    }
  ],
  "7": [
    {
      "time": "06:00",
      "temperature": 2.2
    },
    {
      "time": "07:00",
      "temperature": 2.4
    },
    {
      "time": "08:00",
      "temperature": 3.0
    },
    {
      "time": "09:00",
      "temperature": 3.5
    },
    {
      "time": "10:00",
      "temperature": 4.2
    },
    {
      "time": "11:00",
      "temperature": 4.8
    },
    {
      "time": "12:00",
      "temperature": 5.2
    },
    {
      "time": "13:00",
      "temperature": 4.9
    },
    {
      "time": "14:00",
      "temperature": 4.4
    },
    {
      "time": "15:00",
      "temperature": 4.1
    },
    {
      "time": "16:00",
      "temperature": 4.6
    },
    {
      "time": "17:00",
      "temperature": 4.8
    }
  ],
  "12": [
    {
      "time": "12:00",
      "temperature": 2.4
    },
    {
      "time": "14:00",
      "temperature": 0.3
    },
    {
      "time": "16:00",
      "temperature": -1.4
    },
    {
      "time": "18:00",
      "temperature": -0.7
    },
    {
      "time": "20:00",
      "temperature": -0.8
    },
    {
      "time": "22:00",
      "temperature": -1.5
    },
    {
      "time": "00:00",
      "temperature": -2.2
    },
    {
      "time": "02:00",
      "temperature": -0.4
    },
    {
      "time": "04:00",
      "temperature": 0.8
    },
    {
      "time": "06:00",
      "temperature": 3.2
    },
    {
      "time": "08:00",
      "temperature": 7.4
    },
    {
      "time": "10:00",
      "temperature": 10.4
    },
    {
      "time": "12:00",
      "temperature": 12.2
    },
    {
      "time": "11:52",
      "temperature": 4.8
    },
    {
      "time": "11:53",
      "temperature": 9.6
    },
    {
      "time": "11:54",
      "temperature": 3.2
    },
    {
      "time": "11:57",
      "temperature": 4.8
    },
    {
      "time": "12:13",
      "temperature": 4.8
    },
    {
      "time": "12:23",
      "temperature": 4.8
    },
    {
      "time": "12:29",
      "temperature": 4.8
    }
  ],
  "21": [],
  "101": [
    {
      "time": "06:00",
      "temperature": 4.7
    },
    {
      "time": "07:00",
      "temperature": 5.3
    },
    {
      "time": "08:00",
      "temperature": 5.9
    },
    {
      "time": "09:00",
      "temperature": 6.2
    },
    {
      "time": "10:00",
      "temperature": 6.6
    },
    {
      "time": "11:00",
      "temperature": 6.1
    },
    {
      "time": "12:00",
      "temperature": 5.4
    },
    {
      "time": "13:00",
      "temperature": 4.9
    }
  ]
}
$json_temperature_history$, now())
ON CONFLICT (store_key) DO UPDATE
SET payload = EXCLUDED.payload, updated_at = now();

INSERT INTO app_store (store_key, payload, updated_at)
VALUES ('vehicle-events', $json_vehicle_events$
{
  "1": [
    {
      "type": "TEMPERATURE",
      "title": "Temperatura estable",
      "description": "Lectura dentro de rango: 2.1 °C",
      "time": "17:00",
      "severity": "INFO"
    },
    {
      "type": "NETWORK",
      "title": "Comunicacion correcta",
      "description": "Senal GPS y sensor activos",
      "time": "16:58",
      "severity": "INFO"
    },
    {
      "type": "COOLING",
      "title": "Equipo de frio encendido",
      "description": "Compresor operando normalmente",
      "time": "16:45",
      "severity": "INFO"
    }
  ],
  "2": [
    {
      "type": "TEMPERATURE",
      "title": "Temperatura estable",
      "description": "Lectura dentro de rango: 3.4 °C",
      "time": "17:02",
      "severity": "INFO"
    },
    {
      "type": "DOOR",
      "title": "Puerta cerrada",
      "description": "Cierre confirmado despues de carga",
      "time": "16:50",
      "severity": "INFO"
    },
    {
      "type": "COOLING",
      "title": "Equipo de frio encendido",
      "description": "Compresor manteniendo rango objetivo",
      "time": "16:35",
      "severity": "INFO"
    },
    {
      "type": "NETWORK",
      "title": "Comunicacion correcta",
      "description": "Ultimo paquete recibido sin retraso",
      "time": "16:30",
      "severity": "INFO"
    }
  ],
  "3": [
    {
      "type": "TEMPERATURE",
      "title": "Temperatura fuera de rango",
      "description": "Lectura actual: 6.7 °C",
      "time": "17:01",
      "severity": "WARNING"
    },
    {
      "type": "TEMPERATURE",
      "title": "Tendencia de temperatura alta",
      "description": "Tres lecturas consecutivas sobre 5 °C",
      "time": "16:42",
      "severity": "WARNING"
    },
    {
      "type": "COOLING",
      "title": "Equipo de frio encendido",
      "description": "Compresor activo durante la desviacion",
      "time": "16:20",
      "severity": "INFO"
    }
  ],
  "7": [
    {
      "type": "DOOR",
      "title": "Puerta abierta",
      "description": "Apertura detectada en zona de reparto",
      "time": "17:03",
      "severity": "WARNING"
    },
    {
      "type": "TEMPERATURE",
      "title": "Temperatura cerca del limite",
      "description": "Lectura actual: 4.8 °C",
      "time": "17:00",
      "severity": "WARNING"
    },
    {
      "type": "NETWORK",
      "title": "Comunicacion correcta",
      "description": "Sensor reportando cada minuto",
      "time": "16:55",
      "severity": "INFO"
    }
  ],
  "12": [
    {
      "type": "COOLING",
      "title": "Equipo de frio apagado",
      "description": "Estado recibido por MQTT",
      "time": "11:53",
      "severity": "CRITICAL"
    },
    {
      "type": "DOOR",
      "title": "Puerta abierta",
      "description": "Estado recibido por MQTT",
      "time": "11:53",
      "severity": "WARNING"
    },
    {
      "type": "TEMPERATURE",
      "title": "Temperatura alta: 9.6 °C",
      "description": "Fuera de rango",
      "time": "11:53",
      "severity": "CRITICAL"
    },
    {
      "type": "TEMPERATURE",
      "title": "Temperatura alta: 9.8 °C",
      "description": "Fuera de rango",
      "time": "10:32",
      "severity": "CRITICAL"
    },
    {
      "type": "DOOR",
      "title": "Puerta abierta",
      "description": "Puerta delantera",
      "time": "10:30",
      "severity": "WARNING"
    },
    {
      "type": "COOLING",
      "title": "Equipo de frio encendido",
      "description": "Compresor activado",
      "time": "10:28",
      "severity": "INFO"
    },
    {
      "type": "NETWORK",
      "title": "Comunicacion restablecida",
      "description": "Senal OK",
      "time": "10:27",
      "severity": "INFO"
    }
  ],
  "21": [
    {
      "type": "NETWORK",
      "title": "Sin comunicacion",
      "description": "No se reciben paquetes desde hace 25 min",
      "time": "16:38",
      "severity": "CRITICAL"
    }
  ],
  "101": [
    {
      "type": "TEMPERATURE",
      "title": "Temperatura fuera de rango",
      "description": "Lectura actual: 6.2 °C",
      "time": "16:56",
      "severity": "WARNING"
    },
    {
      "type": "NETWORK",
      "title": "Comunicacion correcta",
      "description": "Unidad reportando desde Trujillo",
      "time": "16:52",
      "severity": "INFO"
    }
  ]
}
$json_vehicle_events$, now())
ON CONFLICT (store_key) DO UPDATE
SET payload = EXCLUDED.payload, updated_at = now();
