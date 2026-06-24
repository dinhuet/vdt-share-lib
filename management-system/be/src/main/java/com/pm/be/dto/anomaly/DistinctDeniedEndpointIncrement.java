package com.pm.be.dto.anomaly;

import java.time.Instant;

public record DistinctDeniedEndpointIncrement(
        int windowSeconds,
        String identityType,
        String identityValue,
        String endpointId,
        Instant eventTimestamp,
        long ttlSeconds) {
}
