package com.pm.be.service.anomaly;

import com.pm.be.dto.anomaly.CurrentMetricValue;
import com.pm.be.enums.AnomalyScopeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentMetricReaderTest {
    @Mock RedisMetricReader redisMetricReader;

    private CurrentMetricReader reader;

    @BeforeEach
    void setUp() {
        reader = new CurrentMetricReader(redisMetricReader);
    }

    @Test
    void read_countMetric_shouldReadNormalizedCounter() {
        Instant timestamp = Instant.parse("2026-06-23T10:00:01Z");
        when(redisMetricReader.windowStart(timestamp, 60)).thenReturn(1782208800L);
        when(redisMetricReader.readCounter(60, 1782208800L, AnomalyScopeType.ENDPOINT,
                "INBOUND_HTTP:endpoint-1", "request_count")).thenReturn(BigDecimal.valueOf(170));

        Optional<CurrentMetricValue> value = reader.read("request_count_1m", 60, AnomalyScopeType.ENDPOINT,
                "INBOUND_HTTP:endpoint-1", timestamp, 1);

        assertThat(value).isPresent();
        assertThat(value.get().value()).isEqualByComparingTo("170");
        assertThat(value.get().rateMetric()).isFalse();
    }

    @Test
    void read_rateMetric_shouldDivideNumeratorByRequestCount() {
        when(redisMetricReader.readCounter(300, 1782208800L, AnomalyScopeType.ENDPOINT,
                "INBOUND_HTTP:endpoint-1", "failed_count")).thenReturn(BigDecimal.valueOf(40));
        when(redisMetricReader.readCounter(300, 1782208800L, AnomalyScopeType.ENDPOINT,
                "INBOUND_HTTP:endpoint-1", "request_count")).thenReturn(BigDecimal.valueOf(100));

        Optional<CurrentMetricValue> value = reader.readAtWindow("error_rate_5m", 300, 1782208800L,
                AnomalyScopeType.ENDPOINT, "INBOUND_HTTP:endpoint-1", 20);

        assertThat(value).isPresent();
        assertThat(value.get().value()).isEqualByComparingTo("0.4");
        assertThat(value.get().rateMetric()).isTrue();
    }

    @Test
    void read_rateMetricWithZeroDenominator_shouldSkip() {
        when(redisMetricReader.readCounter(300, 1782208800L, AnomalyScopeType.ENDPOINT,
                "INBOUND_HTTP:endpoint-1", "failed_count")).thenReturn(BigDecimal.ONE);
        when(redisMetricReader.readCounter(300, 1782208800L, AnomalyScopeType.ENDPOINT,
                "INBOUND_HTTP:endpoint-1", "request_count")).thenReturn(BigDecimal.ZERO);

        assertThat(reader.readAtWindow("error_rate_5m", 300, 1782208800L,
                AnomalyScopeType.ENDPOINT, "INBOUND_HTTP:endpoint-1", 20)).isEmpty();
    }
}
