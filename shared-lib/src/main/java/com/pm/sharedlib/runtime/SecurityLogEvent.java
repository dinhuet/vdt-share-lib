package com.pm.sharedlib.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SecurityLogEvent {
    Instant timestamp;
    String traceId;
    String correlationId;
    String serviceName;
    UUID endpointId;
    String endpointName;
    String flowType;
    String direction;
    String protocol;
    String method;
    String path;
    String targetUrl;
    String topic;
    String clientId;
    String sourceIp;
    String status;
    String resultCode;
    String errorCode;
    String denyReason;
    Long durationMs;
    Integer latencyThresholdMs;
    Integer timeoutMs;
    Integer retryAttempt;
    Integer maxRetries;
    Integer retryDelayMs;
    String failureAction;
    Long rateLimitCurrent;
    Integer rateLimitMax;
    Integer rateLimitWindowSec;
    Integer retentionDays;
    String retentionBucket;
}
