package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;

import java.util.UUID;

public record BaselineRuleDefinition(
        UUID ruleId,
        String ruleCode,
        String metric,
        AnomalyScopeType configuredScopeType,
        String configuredScopeId,
        int historyDays,
        AnomalyTimeBucketType timeBucketType,
        int percentile,
        int minSampleCount,
        int windowSeconds) {
}
