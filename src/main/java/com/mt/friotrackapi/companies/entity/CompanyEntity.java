package com.mt.friotrackapi.companies.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class CompanyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String status;

    @Column(name = "warning_offline_minutes", nullable = false)
    private Integer warningOfflineMinutes = 3;

    @Column(name = "critical_offline_minutes", nullable = false)
    private Integer criticalOfflineMinutes = 10;

    protected CompanyEntity() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getStatus() { return status; }
    public Integer getWarningOfflineMinutes() { return warningOfflineMinutes == null ? 3 : warningOfflineMinutes; }
    public Integer getCriticalOfflineMinutes() { return criticalOfflineMinutes == null ? 10 : criticalOfflineMinutes; }
}
