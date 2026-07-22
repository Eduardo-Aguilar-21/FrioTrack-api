package com.mt.friotrackapi.vehicles.entity;

import com.mt.friotrackapi.companies.entity.CompanyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "vehicles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehicles_company_code", columnNames = {"company_id", "code"}),
        @UniqueConstraint(name = "uk_vehicles_company_plate", columnNames = {"company_id", "plate"}),
        @UniqueConstraint(name = "uk_vehicles_company_device", columnNames = {"company_id", "device_id"})
})
public class VehicleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String plate;
    @Column(nullable = false)
    private String label;
    @Column(nullable = false)
    private String status;
    private String driver;
    @Column(name = "device_id", nullable = false)
    private String deviceId;
    private String model;
    @Column(name = "model_year", nullable = false)
    private Integer year;
    @Column(name = "unit_type")
    private String unitType;
    @Column(name = "load_capacity_kg", nullable = false)
    private Integer loadCapacityKg;
    private Double latitude;
    private Double longitude;
    @Column(name = "current_temperature")
    private String currentTemperature;
    @Column(name = "temperature_state")
    private String temperatureState;
    @Column(name = "door_state")
    private String doorState;
    @Column(name = "cooling_unit_state")
    private String coolingUnitState;
    @Column(name = "last_communication")
    private String lastCommunication;
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
    @Column(name = "detected_protocol")
    private String detectedProtocol;

    protected VehicleEntity() {}

    public VehicleEntity(CompanyEntity company, String code, String plate, String label, String status, String driver, String deviceId, String model, Integer year, String unitType, Integer loadCapacityKg, Double latitude, Double longitude, String currentTemperature, String temperatureState, String doorState, String coolingUnitState, String lastCommunication) {
        this.company = company;
        this.code = code;
        this.plate = plate;
        this.label = label;
        this.status = status;
        this.driver = driver;
        this.deviceId = deviceId;
        this.model = model;
        this.year = year;
        this.unitType = unitType;
        this.loadCapacityKg = loadCapacityKg;
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentTemperature = currentTemperature;
        this.temperatureState = temperatureState;
        this.doorState = doorState;
        this.coolingUnitState = coolingUnitState;
        this.lastCommunication = lastCommunication;
    }

    public Long getId() { return id; }
    public CompanyEntity getCompany() { return company; }
    public String getCode() { return code; }
    public String getPlate() { return plate; }
    public String getLabel() { return label; }
    public String getStatus() { return status; }
    public String getDriver() { return driver; }
    public String getDeviceId() { return deviceId; }
    public String getModel() { return model; }
    public Integer getYear() { return year; }
    public String getUnitType() { return unitType; }
    public Integer getLoadCapacityKg() { return loadCapacityKg; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getCurrentTemperature() { return currentTemperature; }
    public String getTemperatureState() { return temperatureState; }
    public String getDoorState() { return doorState; }
    public String getCoolingUnitState() { return coolingUnitState; }
    public String getLastCommunication() { return lastCommunication; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public String getDetectedProtocol() { return detectedProtocol; }

    public void updateIdentity(CompanyEntity company, String code, String plate, String label, String driver, String deviceId, String model, Integer year, String unitType, Integer loadCapacityKg) {
        this.company = company;
        this.code = code;
        this.plate = plate;
        this.label = label;
        this.driver = driver;
        this.deviceId = deviceId;
        this.model = model;
        this.year = year;
        this.unitType = unitType;
        this.loadCapacityKg = loadCapacityKg;
    }

    public void setStatus(String status) { this.status = status; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public void setDetectedProtocol(String detectedProtocol) { this.detectedProtocol = detectedProtocol; }

    public void updateTelemetry(Double latitude, Double longitude, String currentTemperature, String temperatureState, String doorState, String coolingUnitState, String lastCommunication, Instant lastSeenAt, String status) {
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (currentTemperature != null) this.currentTemperature = currentTemperature;
        if (temperatureState != null) this.temperatureState = temperatureState;
        if (doorState != null) this.doorState = doorState;
        if (coolingUnitState != null) this.coolingUnitState = coolingUnitState;
        if (lastCommunication != null) this.lastCommunication = lastCommunication;
        if (lastSeenAt != null) this.lastSeenAt = lastSeenAt;
        this.status = status;
    }
}
