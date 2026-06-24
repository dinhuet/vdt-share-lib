package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnomalyRuleMatch(
        UUID ruleId,
        String ruleCode,
        String ruleType,
        AnomalySeverity severity,
        String metric,
        AnomalyScopeType scopeType,
        String scopeKey,
        String identity,
        BigDecimal currentValue,
        BigDecimal baselineValue,
        BigDecimal thresholdValue,
        Integer windowSeconds,
        String timeBucket,
        String staticRuleCode,
        String baselineRuleCode,
        UUID notificationRuleId,
        Instant windowStart,
        Instant windowEnd,
        SecurityLogEventMessage event) {

    public static AnomalyRuleMatch fromStatic(StaticRuleMatch match) {
        return new AnomalyRuleMatch(match.ruleId(), match.ruleCode(), "STATIC", match.severity(), match.metric(),
                match.scopeType(), match.scopeKey(), match.identity(), match.currentValue(), null, match.thresholdValue(),
                match.windowSeconds(), null, match.ruleCode(), null, match.notificationRuleId(), match.windowStart(), match.windowEnd(), match.event());
    }

    public static AnomalyRuleMatch fromBaseline(BaselineRuleMatch match) {
        return new AnomalyRuleMatch(match.ruleId(), match.ruleCode(), "BASELINE", match.severity(), match.metric(),
                match.scopeType(), match.scopeKey(), match.identity(), match.currentValue(), match.baselineValue(), match.thresholdValue(),
                match.windowSeconds(), match.timeBucket(), null, match.ruleCode(), match.notificationRuleId(), match.windowStart(), match.windowEnd(), match.event());
    }
}
