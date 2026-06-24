package com.pm.be.dto.response.apidefaultconfig;

import com.pm.be.enums.ApiConfigType;
import com.pm.be.enums.DefaultConfigScope;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiDefaultConfigResponse {
    UUID id;
    ApiConfigType apiType;
    DefaultConfigScope scope;
    UUID microServiceId;
    String microServiceName;
    Integer maxRequests;
    Integer throttleWindowSec;
    Integer maxRequestKb;
    Integer maxResponseKb;
    Integer latencyThresholdMs;
    Integer timeoutMs;
    Integer logRetentionDays;
    Integer maxRetries;
    Integer retryDelayMs;
    String failureAction;
    UUID notificationRuleId;
    Boolean enabled;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
