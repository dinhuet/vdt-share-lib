package com.pm.be.entity.anomaly;

import com.pm.be.enums.AnomalyScopeType;
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
@Table(name = "security_alert_occurrence", indexes = {
        @Index(name = "idx_security_alert_occurrence_alert", columnList = "alert_id"),
        @Index(name = "idx_security_alert_occurrence_rule", columnList = "rule_code, rule_type"),
        @Index(name = "idx_security_alert_occurrence_window", columnList = "window_start, window_end"),
        @Index(name = "idx_security_alert_occurrence_created_at", columnList = "created_at")
})
public class SecurityAlertOccurrenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "alert_id", nullable = false)
    UUID alertId;

    @Column(name = "rule_code", nullable = false, length = 100)
    String ruleCode;

    @Column(name = "rule_type", nullable = false, length = 20)
    String ruleType;

    @Column(nullable = false, length = 100)
    String metric;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    AnomalyScopeType scopeType;

    @Column(name = "scope_key", length = 500)
    String scopeKey;

    @Column(name = "current_value", nullable = false, precision = 19, scale = 4)
    BigDecimal currentValue;

    @Column(name = "threshold_value", nullable = false, precision = 19, scale = 4)
    BigDecimal thresholdValue;

    @Column(name = "baseline_value", precision = 19, scale = 4)
    BigDecimal baselineValue;

    @Column(name = "window_seconds", nullable = false)
    Integer windowSeconds;

    @Column(name = "window_start")
    LocalDateTime windowStart;

    @Column(name = "window_end")
    LocalDateTime windowEnd;

    @Column(name = "time_bucket", length = 50)
    String timeBucket;

    @Column(name = "event_timestamp", nullable = false)
    LocalDateTime eventTimestamp;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
