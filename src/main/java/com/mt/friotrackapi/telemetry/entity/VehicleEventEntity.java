package com.mt.friotrackapi.telemetry.entity;

import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vehicle_events", indexes = @Index(name = "ix_vehicle_events_vehicle_time", columnList = "vehicle_id, occurred_at"))
public class VehicleEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vehicle_id", nullable = false) private VehicleEntity vehicle;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(nullable = false) private String severity;
    protected VehicleEventEntity() {}
    public VehicleEventEntity(VehicleEntity vehicle, String type, String title, String description, Instant occurredAt, String severity) { this.vehicle = vehicle; this.type = type; this.title = title; this.description = description; this.occurredAt = occurredAt; this.severity = severity; }
    public String getType() { return type; } public String getTitle() { return title; } public String getDescription() { return description; } public Instant getOccurredAt() { return occurredAt; } public String getSeverity() { return severity; }
}
