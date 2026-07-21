package com.mt.friotrackapi.telemetry.entity;

import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "telemetry_readings", indexes = {
        @Index(name = "ix_telemetry_readings_vehicle_time", columnList = "vehicle_id, recorded_at"),
        @Index(name = "ix_telemetry_readings_company_time", columnList = "company_id, recorded_at")
})
public class TelemetryReadingEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vehicle_id", nullable = false) private VehicleEntity vehicle;
    @Column(name = "company_id", nullable = false) private Long companyId;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;
    private Double temperature;
    private String humidity;
    @Column(name = "door_state") private String doorState;
    @Column(name = "cooling_unit_state") private String coolingUnitState;
    @Column(name = "fuel_level") private String fuelLevel;
    private String speed;
    private Double latitude;
    private Double longitude;
    @Column(name = "custom_fields", columnDefinition = "TEXT") private String customFields;
    @Column(name = "raw_payload", columnDefinition = "TEXT") private String rawPayload;

    protected TelemetryReadingEntity() {}
    public TelemetryReadingEntity(VehicleEntity vehicle, Long companyId, Instant recordedAt, Double temperature, String humidity, String doorState, String coolingUnitState, String fuelLevel, String speed, Double latitude, Double longitude, String customFields, String rawPayload) {
        this.vehicle = vehicle; this.companyId = companyId; this.recordedAt = recordedAt; this.temperature = temperature; this.humidity = humidity; this.doorState = doorState; this.coolingUnitState = coolingUnitState; this.fuelLevel = fuelLevel; this.speed = speed; this.latitude = latitude; this.longitude = longitude; this.customFields = customFields; this.rawPayload = rawPayload;
    }
    public Long getId() { return id; }
    public VehicleEntity getVehicle() { return vehicle; }
    public Instant getRecordedAt() { return recordedAt; }
    public Double getTemperature() { return temperature; }
    public String getHumidity() { return humidity; }
    public String getDoorState() { return doorState; }
    public String getCoolingUnitState() { return coolingUnitState; }
    public String getFuelLevel() { return fuelLevel; }
    public String getSpeed() { return speed; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getCustomFields() { return customFields; }
    public String getRawPayload() { return rawPayload; }
}
