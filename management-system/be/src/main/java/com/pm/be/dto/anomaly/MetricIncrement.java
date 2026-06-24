package com.pm.be.dto.anomaly;

import com.pm.be.enums.AnomalyScopeType;

import java.time.Instant;

public record MetricIncrement(
        String metric,
        long amount,
        int windowSeconds,
        AnomalyScopeType scopeType,
        String scopeKey,
        Instant eventTimestamp,
        long ttlSeconds) {
}
