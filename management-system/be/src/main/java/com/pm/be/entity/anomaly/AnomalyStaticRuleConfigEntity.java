package com.pm.be.entity.anomaly;

import com.pm.be.enums.AnomalyRuleOperator;
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
@Table(name = "anomaly_static_rule_config")
public class AnomalyStaticRuleConfigEntity {
    @Id
    @Column(name = "rule_id")
    UUID ruleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "rule_id")
    AnomalyRuleEntity rule;

    @Column(name = "threshold_value", nullable = false, precision = 19, scale = 4)
    BigDecimal thresholdValue;

    @Column(name = "window_seconds", nullable = false)
    Integer windowSeconds;

    @Column(name = "min_sample_count", nullable = false)
    Integer minSampleCount;

    @Column(name = "consecutive_windows", nullable = false)
    Integer consecutiveWindows;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    AnomalyRuleOperator operator;
}
