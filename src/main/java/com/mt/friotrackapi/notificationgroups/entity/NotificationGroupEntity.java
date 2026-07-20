package com.mt.friotrackapi.notificationgroups.entity;

import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.users.entity.UserEntity;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "notification_groups", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_groups_company_name", columnNames = {"company_id", "name"})
})
public class NotificationGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "alert_types", nullable = false)
    private String alertTypes;

    @Column(nullable = false)
    private String severities;

    @Column(nullable = false)
    private String channels;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToMany
    @JoinTable(
            name = "notification_group_users",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> users = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "notification_group_vehicles",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "vehicle_id")
    )
    private Set<VehicleEntity> vehicles = new LinkedHashSet<>();

    protected NotificationGroupEntity() {}

    public NotificationGroupEntity(CompanyEntity company, String name, String description, String alertTypes, String severities, String channels, String status) {
        this.company = company;
        this.name = name;
        this.description = description;
        this.alertTypes = alertTypes;
        this.severities = severities;
        this.channels = channels;
        this.status = status;
    }

    public Long getId() { return id; }
    public CompanyEntity getCompany() { return company; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAlertTypes() { return alertTypes; }
    public String getSeverities() { return severities; }
    public String getChannels() { return channels; }
    public String getStatus() { return status; }
    public Set<UserEntity> getUsers() { return users; }
    public Set<VehicleEntity> getVehicles() { return vehicles; }

    public void update(CompanyEntity company, String name, String description, String alertTypes, String severities, String channels, String status) {
        this.company = company;
        this.name = name;
        this.description = description;
        this.alertTypes = alertTypes;
        this.severities = severities;
        this.channels = channels;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
