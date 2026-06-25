package com.pm.be.entity.anomaly;

import com.pm.be.enums.NotificationChannel;
import com.pm.be.enums.NotificationDeliveryStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "notification_delivery", indexes = {
        @Index(name = "idx_notification_delivery_alert", columnList = "alert_id"),
        @Index(name = "idx_notification_delivery_status", columnList = "status"),
        @Index(name = "idx_notification_delivery_created_at", columnList = "created_at")
})
public class NotificationDeliveryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "alert_id", nullable = false)
    UUID alertId;

    @Column(name = "notification_rule_id")
    UUID notificationRuleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    NotificationChannel channel;

    @Column(nullable = false, length = 500)
    String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    NotificationDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    Integer attemptCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    String lastError;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (attemptCount == null) {
            attemptCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
