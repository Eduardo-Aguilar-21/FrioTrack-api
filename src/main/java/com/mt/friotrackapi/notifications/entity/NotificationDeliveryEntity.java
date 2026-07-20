package com.mt.friotrackapi.notifications.entity;

import com.mt.friotrackapi.alerts.entity.AlertEntity;
import com.mt.friotrackapi.notificationgroups.entity.NotificationGroupEntity;
import com.mt.friotrackapi.users.entity.UserEntity;
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
import java.time.Instant;

@Entity
@Table(name = "notification_deliveries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_delivery_once", columnNames = {"alert_id", "user_id", "channel"})
})
public class NotificationDeliveryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private AlertEntity alert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private NotificationGroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String status;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected NotificationDeliveryEntity() {}

    public NotificationDeliveryEntity(AlertEntity alert, NotificationGroupEntity group, UserEntity user, String channel, String status, Instant deliveredAt, String failureReason) {
        this.alert = alert;
        this.group = group;
        this.user = user;
        this.channel = channel;
        this.status = status;
        this.deliveredAt = deliveredAt;
        this.failureReason = failureReason;
    }

    public Long getId() { return id; }
    public AlertEntity getAlert() { return alert; }
    public NotificationGroupEntity getGroup() { return group; }
    public UserEntity getUser() { return user; }
    public String getChannel() { return channel; }
    public String getStatus() { return status; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getReadAt() { return readAt; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }

    public void markRead() {
        this.status = "READ";
        this.readAt = Instant.now();
    }
}
