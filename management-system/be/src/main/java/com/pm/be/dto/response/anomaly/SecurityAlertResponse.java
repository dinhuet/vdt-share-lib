package com.pm.be.dto.response.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.SecurityAlertStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SecurityAlertResponse {
    private UUID id;
    private String alertType;
    private AnomalySeverity severity;
    private SecurityAlertStatus status;
    private String fingerprint;
    private String serviceName;
    private String endpointId;
    private String endpointName;
    private String flowType;
    private String protocol;
    private String clientId;
    private String sourceIp;
    private String resultCode;
    private String metric;
    private AnomalyScopeType scopeType;
    private BigDecimal currentValue;
    private BigDecimal thresholdValue;
    private Integer windowSeconds;
    private Long count;
    private String message;
    private UUID notificationRuleId;
    private Integer cooldownMinutes;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private String ignoredBy;
    private LocalDateTime ignoredAt;
    private LocalDateTime ignoredUntil;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
}
