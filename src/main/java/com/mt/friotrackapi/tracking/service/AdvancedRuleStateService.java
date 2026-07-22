package com.mt.friotrackapi.tracking.service;

import com.mt.friotrackapi.tracking.entity.AdvancedRuleStateEntity;
import com.mt.friotrackapi.tracking.repository.AdvancedRuleStateRepository;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvancedRuleStateService {
    private final AdvancedRuleStateRepository repository;
    @PersistenceContext private EntityManager entityManager;
    public AdvancedRuleStateService(AdvancedRuleStateRepository repository) { this.repository = repository; }

    @Transactional
    public Instant conditionSince(String key, Long vehicleId, Long companyId, String protocol, String type) {
        return state(key, vehicleId, companyId, protocol, type).startCondition();
    }
    @Transactional
    public Instant stoppedSince(String key, Long vehicleId, Long companyId, String protocol, String type) {
        return state(key, vehicleId, companyId, protocol, type).startStopped();
    }
    @Transactional
    public void clearCondition(String key) { repository.findById(key).ifPresent(state -> { state.clearCondition(); if (state.empty()) repository.delete(state); }); }
    @Transactional
    public void clearStopped(String key) { repository.findById(key).ifPresent(state -> { state.clearStopped(); if (state.empty()) repository.delete(state); }); }

    private AdvancedRuleStateEntity state(String key, Long vehicleId, Long companyId, String protocol, String type) {
        return repository.findById(key).orElseGet(() -> repository.save(new AdvancedRuleStateEntity(
                key, entityManager.getReference(VehicleEntity.class, vehicleId), companyId, protocol, type
        )));
    }
}
