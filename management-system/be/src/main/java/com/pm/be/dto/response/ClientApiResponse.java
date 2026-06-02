package com.pm.be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientApiResponse {
    UUID id;
    UUID microServiceId;
    String microServiceName;
    UUID clientId;
    String name;
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
    Boolean deleted;
    LocalDateTime deletedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
