package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientApiRuntimeConfig {
    UUID id;
    UUID microServiceId;
    String serviceName;
    UUID endpointId;
    String endpointKey;
    String apiName;
    String destinationUrl;
    String topic;
    String method;
    String protocol;
    Integer latencyThresholdMs;
    Integer timeoutMs;
    Integer maxRetries;
    Integer retryDelayMs;
    String failureAction;
    Integer logRetentionDays;
    Boolean enabled;
    String syncStatus;
    LocalDateTime lastSyncedAt;
    LocalDateTime updatedAt;
}
