package com.pm.be.dto.response;

import com.pm.be.enums.RegistrationSource;
import com.pm.be.enums.SyncStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExposedApiResponse {
    UUID id;
    UUID microServiceId;
    String microServiceName;
    UUID endpointId;
    String endpointKey;
    String name;
    String path;
    String method;
    String topic;
    String protocol;
    Integer maxRequests;
    Integer throttleWindowSec;
    Integer maxRequestKb;
    Integer maxResponseKb;
    Integer latencyThresholdMs;
    Integer timeoutMs;
    Integer logRetentionDays;
    Boolean useDefaultConfig;
    UUID notificationRuleId;
    Boolean enabled;
    RegistrationSource registrationSource;
    SyncStatus syncStatus;
    LocalDateTime lastSyncedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
