package com.pm.be.entity;

import com.pm.be.enums.RegistrationSource;
import com.pm.be.enums.SyncStatus;
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
@Table(name = "exposed_api", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"micro_service_id", "name"})
})
public class ExposedApiEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "micro_service_id", nullable = false)
    UUID microServiceId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 255)
    String path;

    @Column(length = 10)
    String method;

    @Column(nullable = false, length = 20)
    String protocol;

    @Column(name = "max_requests")
    Integer maxRequests;

    @Column(name = "throttle_window_sec")
    @Builder.Default
    Integer throttleWindowSec = 60;

    @Column(name = "max_request_kb")
    Integer maxRequestKb;

    @Column(name = "max_response_kb")
    Integer maxResponseKb;

    @Column(name = "latency_threshold_ms")
    Integer latencyThresholdMs;

    @Column(name = "timeout_ms")
    @Builder.Default
    Integer timeoutMs = 30000;

    @Column(name = "log_retention_days")
    @Builder.Default
    Integer logRetentionDays = 30;

    @Column(name = "use_default_config")
    @Builder.Default
    Boolean useDefaultConfig = true;

    @Column(name = "notification_rule_id")
    UUID notificationRuleId;

    @Column(name = "is_enabled")
    @Builder.Default
    Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_source", length = 20)
    @Builder.Default
    RegistrationSource registrationSource = RegistrationSource.KAFKA_SYNC;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", length = 20)
    @Builder.Default
    SyncStatus syncStatus = SyncStatus.ACTIVE;

    @Column(name = "last_synced_at")
    LocalDateTime lastSyncedAt;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
