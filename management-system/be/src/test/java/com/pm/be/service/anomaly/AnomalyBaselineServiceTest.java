package com.pm.be.service.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.config.BaselineJobProperties;
import com.pm.be.dto.anomaly.BaselineUpsertRequest;
import com.pm.be.entity.anomaly.AnomalyBaselineEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;
import com.pm.be.repository.anomaly.AnomalyBaselineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyBaselineServiceTest {
    @Mock AnomalyBaselineRepository repository;
    @Mock StringRedisTemplate redisTemplate;

    private AnomalyBaselineService service;
    private BaselineJobProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BaselineJobProperties();
        properties.setCacheEnabled(false);
        service = new AnomalyBaselineService(repository, redisTemplate, new ObjectMapper(), properties);
    }

    @Test
    void upsert_missingExistingBaseline_shouldCreate() {
        BaselineUpsertRequest request = request(BigDecimal.TEN);
        when(repository.findByMetricAndScopeTypeAndScopeKeyAndTimeBucketTypeAndTimeBucketAndHistoryDaysAndAggregationAndWindowSeconds(
                request.metric(), request.scopeType(), request.scopeKey(), request.timeBucketType(), request.timeBucket(),
                request.historyDays(), request.aggregation(), request.windowSeconds())).thenReturn(Optional.empty());
        when(repository.save(any(AnomalyBaselineEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnomalyBaselineEntity saved = service.upsert(request);

        assertThat(saved.getMetric()).isEqualTo("request_count_1m");
        assertThat(saved.getValue()).isEqualByComparingTo("10");
        assertThat(saved.getSampleCount()).isEqualTo(12);
    }

    @Test
    void upsert_existingBaseline_shouldUpdate() {
        BaselineUpsertRequest request = request(BigDecimal.valueOf(25));
        AnomalyBaselineEntity existing = new AnomalyBaselineEntity();
        existing.setId(UUID.randomUUID());
        existing.setValue(BigDecimal.ONE);
        when(repository.findByMetricAndScopeTypeAndScopeKeyAndTimeBucketTypeAndTimeBucketAndHistoryDaysAndAggregationAndWindowSeconds(
                request.metric(), request.scopeType(), request.scopeKey(), request.timeBucketType(), request.timeBucket(),
                request.historyDays(), request.aggregation(), request.windowSeconds())).thenReturn(Optional.of(existing));
        when(repository.save(any(AnomalyBaselineEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnomalyBaselineEntity saved = service.upsert(request);

        assertThat(saved.getId()).isEqualTo(existing.getId());
        assertThat(saved.getValue()).isEqualByComparingTo("25");
    }

    @Test
    void cacheKey_shouldMatchPhase4Format() {
        BaselineUpsertRequest request = request(BigDecimal.TEN);

        assertThat(service.cacheKey(request)).isEqualTo("vdt:anomaly:baseline:request_count_1m:ENDPOINT:INBOUND_HTTP:endpoint-1:SAME_HOUR:HOUR_10:7:P95:60");
    }

    private BaselineUpsertRequest request(BigDecimal value) {
        return new BaselineUpsertRequest(UUID.randomUUID(), "request_count_1m", AnomalyScopeType.ENDPOINT, "INBOUND_HTTP:endpoint-1",
                AnomalyTimeBucketType.SAME_HOUR, "HOUR_10", 7, 95, "P95", value, 12,
                LocalDateTime.parse("2026-06-24T10:05:00"), 60);
    }
}
