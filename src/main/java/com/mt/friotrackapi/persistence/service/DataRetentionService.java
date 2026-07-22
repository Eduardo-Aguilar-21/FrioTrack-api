package com.mt.friotrackapi.persistence.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataRetentionService {
    @PersistenceContext private EntityManager entityManager;
    @Value("${friotrack.retention.telemetry-days:180}") private int telemetryDays;
    @Value("${friotrack.retention.events-days:365}") private int eventDays;
    @Value("${friotrack.retention.push-days:180}") private int pushDays;
    @Value("${friotrack.retention.trips-days:730}") private int tripDays;

    @Scheduled(cron = "${friotrack.retention.cron:0 20 3 * * *}")
    @Transactional
    public void purgeExpiredData() {
        entityManager.createNativeQuery("DELETE FROM telemetry_readings WHERE recorded_at < :cutoff").setParameter("cutoff", Instant.now().minusSeconds(days(telemetryDays))).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM vehicle_events WHERE occurred_at < :cutoff").setParameter("cutoff", Instant.now().minusSeconds(days(eventDays))).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM mobile_push_notifications WHERE created_at < :cutoff").setParameter("cutoff", Instant.now().minusSeconds(days(pushDays))).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM advanced_rule_states WHERE updated_at < :cutoff").setParameter("cutoff", Instant.now().minusSeconds(days(7))).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM tracked_trips WHERE status <> 'IN_PROGRESS' AND ended_at < :cutoff").setParameter("cutoff", Instant.now().minusSeconds(days(tripDays))).executeUpdate();
    }
    private long days(int value) { return Math.max(1, value) * 86400L; }
}
