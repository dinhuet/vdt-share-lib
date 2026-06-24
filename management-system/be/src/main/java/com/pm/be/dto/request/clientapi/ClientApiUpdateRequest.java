package com.pm.be.dto.request.clientapi;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientApiUpdateRequest {
    UUID microServiceId;
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
}
