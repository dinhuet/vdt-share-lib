package com.pm.be.dto.anomaly;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLogEventMessage {
    private Instant timestamp;
    private String serviceName;
    private String endpointId;
    private String endpointName;
    private String flowType;
    private String direction;
    private String protocol;
    private String method;
    private String path;
    private String targetUrl;
    private String topic;
    private String clientId;
    private String sourceIp;
    private String status;
    private String resultCode;
    private String errorCode;
    private Long durationMs;
    private Long latencyThresholdMs;
    private Long timeoutMs;
    private Integer retryAttempt;
    private Integer maxRetries;
    private Long rateLimitCurrent;
    private Long rateLimitMax;
    private Integer rateLimitWindowSec;
}
