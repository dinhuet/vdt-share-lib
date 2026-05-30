package com.pm.be.entity;

import com.pm.be.enums.DefaultConfigScope;
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
@Table(name = "api_default_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"scope", "micro_service_id"})
})
public class ApiDefaultConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    DefaultConfigScope scope;

    @Column(name = "micro_service_id")
    UUID microServiceId;

    @Column(name = "max_requests")
    Integer maxRequests;

    @Column(name = "throttle_window_sec")
    Integer throttleWindowSec;

    @Column(name = "max_request_kb")
    Integer maxRequestKb;

    @Column(name = "max_response_kb")
    Integer maxResponseKb;

    @Column(name = "latency_threshold_ms")
    Integer latencyThresholdMs;

    @Column(name = "timeout_ms")
    Integer timeoutMs;

    @Column(name = "log_retention_days")
    Integer logRetentionDays;

    @Column(name = "is_enabled")
    @Builder.Default
    Boolean enabled = true;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
