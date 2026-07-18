package com.mt.friotrackapi.sensors.entity;

import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "sensor_parameters", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sensor_parameters_company_code", columnNames = {"company_id", "code"})
})
public class SensorParameterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicle;

    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String type;
    private String unit;
    @Column(name = "last_value", nullable = false)
    private String lastValue;
    @Column(nullable = false)
    private String status;

    protected SensorParameterEntity() {}

    public SensorParameterEntity(CompanyEntity company, VehicleEntity vehicle, String code, String type, String unit, String lastValue, String status) {
        this.company = company;
        this.vehicle = vehicle;
        this.code = code;
        this.type = type;
        this.unit = unit;
        this.lastValue = lastValue;
        this.status = status;
    }

    public Long getId() { return id; }
    public CompanyEntity getCompany() { return company; }
    public VehicleEntity getVehicle() { return vehicle; }
    public String getCode() { return code; }
    public String getType() { return type; }
    public String getUnit() { return unit; }
    public String getLastValue() { return lastValue; }
    public String getStatus() { return status; }

    public void update(CompanyEntity company, VehicleEntity vehicle, String code, String type, String unit) {
        this.company = company;
        this.vehicle = vehicle;
        this.code = code;
        this.type = type;
        this.unit = unit;
    }

    public void setStatus(String status) { this.status = status; }
}
