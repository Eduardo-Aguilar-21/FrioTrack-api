package com.mt.friotrackapi.alerts.entity;

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

@Entity
@Table(name = "alerts")
public class AlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false)
    private String type;
    @Column(nullable = false)
    private String severity;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(name = "vehicle_label", nullable = false)
    private String vehicleLabel;
    @Column(name = "vehicle_code", nullable = false)
    private String vehicleCode;
    @Column(name = "occurred_at_label", nullable = false)
    private String occurredAtLabel;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private String duration;
    private String reading;
    private String icon;

    protected AlertEntity() {}

    public AlertEntity(CompanyEntity company, String type, String severity, String title, String description, String vehicleLabel, String vehicleCode, String occurredAtLabel, String status, String duration, String reading, String icon) {
        this.company = company;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.vehicleLabel = vehicleLabel;
        this.vehicleCode = vehicleCode;
        this.occurredAtLabel = occurredAtLabel;
        this.status = status;
        this.duration = duration;
        this.reading = reading;
        this.icon = icon;
    }

    public Long getId() { return id; }
    public CompanyEntity getCompany() { return company; }
    public String getType() { return type; }
    public String getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVehicleLabel() { return vehicleLabel; }
    public String getVehicleCode() { return vehicleCode; }
    public String getOccurredAtLabel() { return occurredAtLabel; }
    public String getStatus() { return status; }
    public String getDuration() { return duration; }
    public String getReading() { return reading; }
    public String getIcon() { return icon; }

    public void updateStatus(String status) { this.status = status; }

    public void updateMqtt(String severity, String title, String description, String vehicleLabel, String occurredAtLabel, String status, String duration, String reading, String icon) {
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.vehicleLabel = vehicleLabel;
        this.occurredAtLabel = occurredAtLabel;
        this.status = status;
        this.duration = duration;
        this.reading = reading;
        this.icon = icon;
    }
}
