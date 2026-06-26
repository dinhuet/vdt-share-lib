package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StaticRuleMatch(
        UUID ruleId,
        String ruleCode,
        AnomalySeverity severity,
        String metric,
        AnomalyScopeType scopeType,
        String scopeKey,
        String identity,
        BigDecimal currentValue,
        BigDecimal thresholdValue,
        Integer windowSeconds,
        Instant windowStart,
        Instant windowEnd,
        UUID notificationRuleId,
        Integer cooldownMinutes,
        SecurityLogEventMessage event) {
}
