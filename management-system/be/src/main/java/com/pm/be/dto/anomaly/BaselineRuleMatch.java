package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BaselineRuleMatch(
        UUID ruleId,
        String ruleCode,
        AnomalySeverity severity,
        String metric,
        AnomalyScopeType scopeType,
        String scopeKey,
        String identity,
        BigDecimal currentValue,
        BigDecimal baselineValue,
        BigDecimal thresholdValue,
        BigDecimal multiplier,
        BigDecimal minAbsoluteThreshold,
        Integer windowSeconds,
        String timeBucket,
        UUID notificationRuleId,
        Integer cooldownMinutes,
        Instant windowStart,
        Instant windowEnd,
        SecurityLogEventMessage event) {
}
