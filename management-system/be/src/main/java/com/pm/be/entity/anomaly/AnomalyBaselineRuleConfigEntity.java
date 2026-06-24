package com.pm.be.entity.anomaly;

import com.pm.be.enums.AnomalyTimeBucketType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "anomaly_baseline_rule_config")
public class AnomalyBaselineRuleConfigEntity {
    @Id
    @Column(name = "rule_id")
    UUID ruleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "rule_id")
    AnomalyRuleEntity rule;

    @Column(name = "history_days", nullable = false)
    Integer historyDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_bucket_type", nullable = false, length = 30)
    AnomalyTimeBucketType timeBucketType;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal percentile;

    @Column(nullable = false, precision = 10, scale = 4)
    BigDecimal multiplier;

    @Column(name = "min_absolute_threshold", nullable = false, precision = 19, scale = 4)
    BigDecimal minAbsoluteThreshold;

    @Column(name = "max_absolute_threshold", precision = 19, scale = 4)
    BigDecimal maxAbsoluteThreshold;

    @Column(name = "min_sample_count", nullable = false)
    Integer minSampleCount;

    @Column(name = "consecutive_windows", nullable = false)
    Integer consecutiveWindows;
}
