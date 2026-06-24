package com.pm.be.entity.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;
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
@Table(name = "anomaly_baseline",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "metric", "scope_type", "scope_key", "time_bucket_type", "time_bucket",
                "history_days", "aggregation", "window_seconds"
        }),
        indexes = {
                @Index(name = "idx_anomaly_baseline_lookup", columnList = "metric,scope_type,scope_key,time_bucket"),
                @Index(name = "idx_anomaly_baseline_calculated_at", columnList = "calculated_at")
        })
public class AnomalyBaselineEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "rule_id")
    UUID ruleId;

    @Column(nullable = false, length = 100)
    String metric;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    AnomalyScopeType scopeType;

    @Column(name = "scope_key", nullable = false, length = 500)
    String scopeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_bucket_type", nullable = false, length = 30)
    AnomalyTimeBucketType timeBucketType;

    @Column(name = "time_bucket", nullable = false, length = 50)
    String timeBucket;

    @Column(name = "history_days", nullable = false)
    Integer historyDays;

    @Column
    Integer percentile;

    @Column(nullable = false, length = 20)
    String aggregation;

    @Column(nullable = false, precision = 19, scale = 6)
    BigDecimal value;

    @Column(name = "sample_count", nullable = false)
    Long sampleCount;

    @Column(name = "calculated_at", nullable = false)
    LocalDateTime calculatedAt;

    @Column(name = "window_seconds", nullable = false)
    Integer windowSeconds;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
