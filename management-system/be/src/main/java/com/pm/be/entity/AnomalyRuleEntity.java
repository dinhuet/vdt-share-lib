package com.pm.be.entity;

import com.pm.be.enums.AnomalyRuleType;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
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
@Table(name = "anomaly_rule", uniqueConstraints = {
        @UniqueConstraint(columnNames = "rule_code")
})
public class AnomalyRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "rule_code", nullable = false, length = 100)
    String ruleCode;

    @Column(nullable = false, length = 200)
    String name;

    @Column(length = 1000)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    AnomalyRuleType ruleType;

    @Column(nullable = false, length = 100)
    String metric;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AnomalySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    AnomalyScopeType scopeType;

    @Column(name = "scope_id", length = 200)
    String scopeId;

    @Column(nullable = false)
    @Builder.Default
    Boolean enabled = true;

    @Column(name = "notification_rule_id")
    UUID notificationRuleId;

    @Column(name = "cooldown_minutes")
    Integer cooldownMinutes;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @OneToOne(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    AnomalyStaticRuleConfigEntity staticConfig;

    @OneToOne(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    AnomalyBaselineRuleConfigEntity baselineConfig;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
