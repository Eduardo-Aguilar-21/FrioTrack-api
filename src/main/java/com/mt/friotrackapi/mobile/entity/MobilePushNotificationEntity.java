package com.mt.friotrackapi.mobile.entity;

import com.mt.friotrackapi.alerts.entity.AlertEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mobile_push_notifications", uniqueConstraints = @UniqueConstraint(name = "uk_mobile_push_once", columnNames = {"alert_id", "mobile_token"}))
public class MobilePushNotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "alert_id", nullable = false) private AlertEntity alert;
    @Column(name = "company_id", nullable = false) private Long companyId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "mobile_token", nullable = false) private String mobileToken;
    @Column(name = "push_token", nullable = false) private String pushToken;
    @Column(nullable = false) private String status;
    @Column(name = "ticket_id") private String ticketId;
    @Column(name = "failure_reason", columnDefinition = "TEXT") private String failureReason;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "received_at") private Instant receivedAt;
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    protected MobilePushNotificationEntity() {}
    public MobilePushNotificationEntity(AlertEntity alert, Long companyId, Long userId, String mobileToken, String pushToken) { this.alert = alert; this.companyId = companyId; this.userId = userId; this.mobileToken = mobileToken; this.pushToken = pushToken; this.status = "PENDING"; }
    public void sent(String ticketId) { this.status = "SENT"; this.ticketId = ticketId; this.sentAt = Instant.now(); }
    public void failed(String reason) { this.status = "FAILED"; this.failureReason = reason; this.sentAt = Instant.now(); }
    public void received() { if (!"READ".equals(this.status)) this.status = "RECEIVED"; this.receivedAt = Instant.now(); }
    public void read() { this.status = "READ"; this.readAt = Instant.now(); if (this.receivedAt == null) this.receivedAt = Instant.now(); }
}
