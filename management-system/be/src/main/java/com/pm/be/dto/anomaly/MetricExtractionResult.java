package com.pm.be.dto.anomaly;

import java.util.List;

public record MetricExtractionResult(
        List<MetricIncrement> metricIncrements,
        List<DistinctDeniedEndpointIncrement> distinctDeniedEndpointIncrements) {

    public static MetricExtractionResult empty() {
        return new MetricExtractionResult(List.of(), List.of());
    }
}
