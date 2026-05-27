package com.pm.be.entity;

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
@Table(name = "client_api", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"micro_service_id", "name"})
})
public class ClientApiEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "micro_service_id", nullable = false)
    UUID microServiceId;

    @Column(name = "client_id")
    UUID clientId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "destination_url", length = 255)
    String destinationUrl;

    @Column(length = 10)
    String method;

    @Column(nullable = false, length = 20)
    String protocol;

    @Column(name = "latency_threshold_ms")
    Integer latencyThresholdMs;

    @Column(name = "timeout_ms")
    @Builder.Default
    Integer timeoutMs = 30000;

    @Column(name = "max_retries")
    @Builder.Default
    Integer maxRetries = 3;

    @Column(name = "retry_delay_ms")
    @Builder.Default
    Integer retryDelayMs = 1000;

    @Column(name = "failure_action", length = 50)
    String failureAction;

    @Column(name = "log_retention_days")
    @Builder.Default
    Integer logRetentionDays = 30;

    @Column(name = "notification_rule_id")
    UUID notificationRuleId;

    @Column(name = "is_enabled")
    @Builder.Default
    Boolean enabled = true;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
