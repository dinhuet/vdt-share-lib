package com.pm.be.dto.anomaly;

import java.math.BigDecimal;

public record BaselineCalculationResult(BigDecimal value, long sampleCount) {
}
