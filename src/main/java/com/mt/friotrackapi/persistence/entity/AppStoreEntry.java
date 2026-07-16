package com.mt.friotrackapi.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "app_store")
public class AppStoreEntry {

    @Id
    @Column(name = "store_key", nullable = false, length = 120)
    private String key;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppStoreEntry() {
    }

    public AppStoreEntry(String key, String payload) {
        this.key = key;
        this.payload = payload;
        this.updatedAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updatePayload(String payload) {
        this.payload = payload;
        this.updatedAt = Instant.now();
    }
}
