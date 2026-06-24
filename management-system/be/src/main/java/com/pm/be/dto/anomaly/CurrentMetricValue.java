package com.pm.be.dto.anomaly;

import java.math.BigDecimal;

public record CurrentMetricValue(
        String metric,
        BigDecimal value,
        BigDecimal numerator,
        BigDecimal denominator,
        boolean rateMetric) {
}
