package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.AnomalyTimeBucketType;

import java.math.BigDecimal;
import java.util.UUID;

public record BaselineRuleDefinition(
        UUID ruleId,
        String ruleCode,
        String metric,
        AnomalySeverity severity,
        AnomalyScopeType configuredScopeType,
        String configuredScopeId,
        UUID notificationRuleId,
        Integer cooldownMinutes,
        int historyDays,
        AnomalyTimeBucketType timeBucketType,
        int percentile,
        BigDecimal multiplier,
        BigDecimal minAbsoluteThreshold,
        BigDecimal maxAbsoluteThreshold,
        int minSampleCount,
        int consecutiveWindows,
        int windowSeconds) {
}
