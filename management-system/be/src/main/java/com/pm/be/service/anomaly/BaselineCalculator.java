package com.pm.be.service.anomaly;

import com.pm.be.dto.anomaly.BaselineCalculationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Component
public class BaselineCalculator {
    public Optional<BaselineCalculationResult> percentile(List<Double> values, int percentile) {
        List<Double> sorted = cleaned(values);
        if (sorted.isEmpty() || percentile < 1 || percentile > 100) {
            return Optional.empty();
        }
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        return Optional.of(result(sorted.get(Math.max(0, index)), sorted.size()));
    }

    public Optional<BaselineCalculationResult> average(List<Double> values) {
        List<Double> cleaned = cleaned(values);
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        double sum = cleaned.stream().mapToDouble(Double::doubleValue).sum();
        return Optional.of(result(sum / cleaned.size(), cleaned.size()));
    }

    private List<Double> cleaned(List<Double> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && Double.isFinite(value))
                .sorted()
                .toList();
    }

    private BaselineCalculationResult result(double value, long sampleCount) {
        return new BaselineCalculationResult(BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP), sampleCount);
    }
}
