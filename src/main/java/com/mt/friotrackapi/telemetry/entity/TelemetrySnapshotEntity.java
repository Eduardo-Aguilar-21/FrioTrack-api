package com.mt.friotrackapi.telemetry.entity;

import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "telemetry_snapshots")
public class TelemetrySnapshotEntity {
    @Id
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicle;

    private String temperature;
    @Column(name = "temperature_state") private String temperatureState;
    private String humidity;
    @Column(name = "door_state") private String doorState;
    @Column(name = "cooling_unit_state") private String coolingUnitState;
    @Column(name = "fuel_level") private String fuelLevel;
    private String speed;
    @Column(name = "target_range") private String targetRange;
    private Double latitude;
    private Double longitude;
    private String address;
    @Column(name = "last_communication") private String lastCommunication;
    @Column(name = "custom_fields", columnDefinition = "TEXT") private String customFields;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected TelemetrySnapshotEntity() {}
    public TelemetrySnapshotEntity(VehicleEntity vehicle) { this.vehicle = vehicle; this.vehicleId = vehicle.getId(); }

    public VehicleEntity getVehicle() { return vehicle; }
    public String getTemperature() { return temperature; }
    public String getTemperatureState() { return temperatureState; }
    public String getHumidity() { return humidity; }
    public String getDoorState() { return doorState; }
    public String getCoolingUnitState() { return coolingUnitState; }
    public String getFuelLevel() { return fuelLevel; }
    public String getSpeed() { return speed; }
    public String getTargetRange() { return targetRange; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getLastCommunication() { return lastCommunication; }
    public String getCustomFields() { return customFields; }

    public void update(String temperature, String temperatureState, String humidity, String doorState, String coolingUnitState, String fuelLevel, String speed, String targetRange, Double latitude, Double longitude, String address, String lastCommunication, String customFields) {
        this.temperature = temperature;
        this.temperatureState = temperatureState;
        this.humidity = humidity;
        this.doorState = doorState;
        this.coolingUnitState = coolingUnitState;
        this.fuelLevel = fuelLevel;
        this.speed = speed;
        this.targetRange = targetRange;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.lastCommunication = lastCommunication;
        this.customFields = customFields;
        this.updatedAt = Instant.now();
    }
}
