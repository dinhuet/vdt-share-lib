package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.SecurityAlertStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SecurityAnomalyEvent {
    private Instant timestamp;
    private UUID alertId;
    private String eventType;
    private String ruleCode;
    private String ruleType;
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
    private Instant windowStart;
    private Instant windowEnd;
    private String message;
}
