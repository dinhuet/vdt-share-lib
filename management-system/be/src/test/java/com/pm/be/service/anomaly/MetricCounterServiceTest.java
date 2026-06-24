package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyDetectorProperties;
import com.pm.be.dto.anomaly.DistinctDeniedEndpointIncrement;
import com.pm.be.dto.anomaly.MetricExtractionResult;
import com.pm.be.dto.anomaly.MetricIncrement;
import com.pm.be.enums.AnomalyScopeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricCounterServiceTest {
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock SetOperations<String, String> setOperations;

    private MetricCounterService service;

    @BeforeEach
    void setUp() {
        service = new MetricCounterService(redisTemplate, new AnomalyDetectorProperties());
    }

    @Test
    void buildCounterKey_shouldUseExpectedFormatAndWindowStart() {
        var increment = increment();

        String key = service.buildCounterKey(increment);

        assertThat(service.windowStart(increment.eventTimestamp().getEpochSecond(), 60)).isEqualTo(1782208800L);
        assertThat(key).isEqualTo("vdt:anomaly:counter:60:1782208800:ENDPOINT_CLIENT:INBOUND_HTTP:endpoint-1:client:client-a:auth_fail_count");
    }

    @Test
    void increment_shouldIncrementCounterAndSetTtl() {
        var increment = increment();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.increment(new MetricExtractionResult(List.of(increment), List.of()));

        String key = service.buildCounterKey(increment);
        verify(valueOperations).increment(key, 1L);
        verify(redisTemplate).expire(key, Duration.ofSeconds(120));
    }

    @Test
    void increment_shouldAddDistinctDeniedEndpointAndSetTtl() {
        var distinct = new DistinctDeniedEndpointIncrement(
                300,
                "client",
                "client-a",
                "endpoint-1",
                Instant.parse("2026-06-23T10:00:01Z"),
                600);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        service.increment(new MetricExtractionResult(List.of(), List.of(distinct)));

        String key = service.buildDistinctDeniedEndpointSetKey(distinct);
        verify(setOperations).add(key, "endpoint-1");
        verify(redisTemplate).expire(key, Duration.ofSeconds(600));
    }

    private MetricIncrement increment() {
        return new MetricIncrement(
                "auth_fail_count",
                1,
                60,
                AnomalyScopeType.ENDPOINT_CLIENT,
                "INBOUND_HTTP:endpoint-1:client:client-a",
                Instant.parse("2026-06-23T10:00:01Z"),
                120);
    }
}
