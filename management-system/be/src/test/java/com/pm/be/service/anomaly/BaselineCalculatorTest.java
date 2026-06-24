package com.pm.be.service.anomaly;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BaselineCalculatorTest {
    private final BaselineCalculator calculator = new BaselineCalculator();

    @Test
    void percentile_shouldUseNearestRank() {
        var result = calculator.percentile(List.of(1.0, 2.0, 3.0, 4.0, 5.0), 95);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualByComparingTo("5.000000");
        assertThat(result.get().sampleCount()).isEqualTo(5);
    }

    @Test
    void average_shouldCalculateMeanAndSampleCount() {
        var result = calculator.average(List.of(2.0, 4.0, 6.0));

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualByComparingTo("4.000000");
        assertThat(result.get().sampleCount()).isEqualTo(3);
    }

    @Test
    void percentile_emptyValues_shouldReturnEmpty() {
        assertThat(calculator.percentile(List.of(), 95)).isEmpty();
    }
}
