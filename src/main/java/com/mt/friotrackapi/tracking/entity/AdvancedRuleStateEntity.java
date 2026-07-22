package com.mt.friotrackapi.tracking.entity;

import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "advanced_rule_states")
public class AdvancedRuleStateEntity {
    @Id @Column(name = "state_key", length = 220) private String stateKey;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vehicle_id", nullable = false) private VehicleEntity vehicle;
    @Column(name = "company_id", nullable = false) private Long companyId;
    @Column(nullable = false) private String protocol;
    @Column(name = "rule_type", nullable = false) private String ruleType;
    @Column(name = "condition_since") private Instant conditionSince;
    @Column(name = "stopped_since") private Instant stoppedSince;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected AdvancedRuleStateEntity() {}
    public AdvancedRuleStateEntity(String stateKey, VehicleEntity vehicle, Long companyId, String protocol, String ruleType) {
        this.stateKey = stateKey; this.vehicle = vehicle; this.companyId = companyId; this.protocol = protocol; this.ruleType = ruleType; this.updatedAt = Instant.now();
    }
    public Instant getConditionSince() { return conditionSince; }
    public Instant getStoppedSince() { return stoppedSince; }
    public Instant startCondition() { if (conditionSince == null) conditionSince = Instant.now(); updatedAt = Instant.now(); return conditionSince; }
    public Instant startStopped() { if (stoppedSince == null) stoppedSince = Instant.now(); updatedAt = Instant.now(); return stoppedSince; }
    public void clearCondition() { conditionSince = null; updatedAt = Instant.now(); }
    public void clearStopped() { stoppedSince = null; updatedAt = Instant.now(); }
    public boolean empty() { return conditionSince == null && stoppedSince == null; }
}
