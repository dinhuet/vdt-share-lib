package com.pm.sharedlib.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ExposedApiRuntimeConfig {
    UUID id;
    UUID microServiceId;
    String serviceName;
    UUID endpointId;
    String endpointKey;
    String apiName;
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
    String syncStatus;
    String lastSyncedAt;
    String updatedAt;
}
