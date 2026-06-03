package com.pm.be.dto.response;

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
public class ClientApiRedisConfig {
    UUID id;
    UUID microServiceId;
    String serviceName;
    UUID clientId;
    String apiName;
    String destinationUrl;
    String method;
    String protocol;
    Integer latencyThresholdMs;
    Integer timeoutMs;
    Integer maxRetries;
    Integer retryDelayMs;
    String failureAction;
    Integer logRetentionDays;
    UUID notificationRuleId;
    Boolean enabled;
    SyncStatus syncStatus;
    LocalDateTime lastSyncedAt;
    LocalDateTime updatedAt;
}
