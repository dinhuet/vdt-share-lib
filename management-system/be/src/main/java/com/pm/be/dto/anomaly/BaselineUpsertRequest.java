package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BaselineUpsertRequest(
        UUID ruleId,
        String metric,
        AnomalyScopeType scopeType,
        String scopeKey,
        AnomalyTimeBucketType timeBucketType,
        String timeBucket,
        int historyDays,
        Integer percentile,
        String aggregation,
        BigDecimal value,
        long sampleCount,
        LocalDateTime calculatedAt,
        int windowSeconds) {
}
