package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyRuleOperator;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;

import java.math.BigDecimal;
import java.util.UUID;

public record StaticRuleDefinition(
        UUID ruleId,
        String ruleCode,
        String metric,
        AnomalySeverity severity,
        AnomalyScopeType scopeType,
        UUID notificationRuleId,
        Integer cooldownMinutes,
        BigDecimal thresholdValue,
        Integer windowSeconds,
        Integer minSampleCount,
        Integer consecutiveWindows,
        AnomalyRuleOperator operator) {
}
