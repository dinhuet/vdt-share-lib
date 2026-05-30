package com.pm.be.dto.request;

import com.pm.be.enums.DefaultApplyMode;
import com.pm.be.enums.DefaultConfigScope;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiDefaultConfigUpsertRequest {
    DefaultConfigScope scope;
    UUID microServiceId;
    Integer maxRequests;
    Integer throttleWindowSec;
    Integer maxRequestKb;
    Integer maxResponseKb;
    Integer latencyThresholdMs;
    Integer timeoutMs;
    Integer logRetentionDays;
    Boolean enabled;
    DefaultApplyMode applyMode;
}
