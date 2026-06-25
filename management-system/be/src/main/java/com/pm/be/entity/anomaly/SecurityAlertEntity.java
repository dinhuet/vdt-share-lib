package com.pm.be.entity.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.SecurityAlertStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "security_alert", indexes = {
        @Index(name = "idx_security_alert_fingerprint", columnList = "fingerprint"),
        @Index(name = "idx_security_alert_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_security_alert_endpoint", columnList = "endpoint_id"),
        @Index(name = "idx_security_alert_client", columnList = "client_id"),
        @Index(name = "idx_security_alert_source_ip", columnList = "source_ip")
})
public class SecurityAlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "alert_type", nullable = false, length = 100)
    String alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AnomalySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    SecurityAlertStatus status;

    @Column(nullable = false, length = 500)
    String fingerprint;

    @Column(name = "service_name", length = 255)
    String serviceName;

    @Column(name = "endpoint_id", length = 200)
    String endpointId;

    @Column(name = "endpoint_name", length = 255)
    String endpointName;

    @Column(name = "flow_type", length = 50)
    String flowType;

    @Column(length = 20)
    String protocol;

    @Column(name = "client_id", length = 200)
    String clientId;

    @Column(name = "source_ip", length = 64)
    String sourceIp;

    @Column(name = "result_code", length = 100)
    String resultCode;

    @Column(nullable = false, length = 100)
    String metric;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    AnomalyScopeType scopeType;

    @Column(name = "current_value", nullable = false, precision = 19, scale = 4)
    BigDecimal currentValue;

    @Column(name = "threshold_value", nullable = false, precision = 19, scale = 4)
    BigDecimal thresholdValue;

    @Column(name = "window_seconds", nullable = false)
    Integer windowSeconds;

    @Column(nullable = false)
    Long count;

    @Column(length = 2000)
    String message;

    @Column(name = "notification_rule_id")
    UUID notificationRuleId;

    @Column(name = "first_seen_at")
    LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    LocalDateTime lastSeenAt;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "acknowledged_by", length = 100)
    String acknowledgedBy;

    @Column(name = "acknowledged_at")
    LocalDateTime acknowledgedAt;

    @Column(name = "ignored_by", length = 100)
    String ignoredBy;

    @Column(name = "ignored_at")
    LocalDateTime ignoredAt;

    @Column(name = "ignored_until")
    LocalDateTime ignoredUntil;

    @Column(name = "resolved_by", length = 100)
    String resolvedBy;

    @Column(name = "resolved_at")
    LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (firstSeenAt == null) {
            firstSeenAt = now;
        }
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
