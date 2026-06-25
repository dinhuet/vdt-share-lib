package com.pm.be.entity.anomaly;

import com.pm.be.enums.SecurityAlertActionTargetType;
import com.pm.be.enums.SecurityAlertActionType;
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
@Table(name = "security_alert_action", indexes = {
        @Index(name = "idx_security_alert_action_alert", columnList = "alert_id"),
        @Index(name = "idx_security_alert_action_created_at", columnList = "created_at")
})
public class SecurityAlertActionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "alert_id", nullable = false)
    UUID alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    SecurityAlertActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    SecurityAlertActionTargetType targetType;

    @Column(name = "target_value", length = 255)
    String targetValue;

    @Column(name = "duration_minutes")
    Integer durationMinutes;

    @Column(columnDefinition = "TEXT")
    String reason;

    @Column(name = "created_by", nullable = false, length = 100)
    String createdBy;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
