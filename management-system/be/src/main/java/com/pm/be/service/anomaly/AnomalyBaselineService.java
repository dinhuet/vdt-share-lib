package com.pm.be.service.anomaly;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.config.BaselineJobProperties;
import com.pm.be.dto.anomaly.BaselineUpsertRequest;
import com.pm.be.entity.anomaly.AnomalyBaselineEntity;
import com.pm.be.repository.anomaly.AnomalyBaselineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyBaselineService {
    private final AnomalyBaselineRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final BaselineJobProperties properties;

    @Transactional
    public AnomalyBaselineEntity upsert(BaselineUpsertRequest request) {
        validate(request);
        AnomalyBaselineEntity entity = repository.findByMetricAndScopeTypeAndScopeKeyAndTimeBucketTypeAndTimeBucketAndHistoryDaysAndAggregationAndWindowSeconds(
                        request.metric(), request.scopeType(), request.scopeKey(), request.timeBucketType(), request.timeBucket(),
                        request.historyDays(), request.aggregation(), request.windowSeconds())
                .orElseGet(AnomalyBaselineEntity::new);
        entity.setRuleId(request.ruleId());
        entity.setMetric(request.metric());
        entity.setScopeType(request.scopeType());
        entity.setScopeKey(request.scopeKey());
        entity.setTimeBucketType(request.timeBucketType());
        entity.setTimeBucket(request.timeBucket());
        entity.setHistoryDays(request.historyDays());
        entity.setPercentile(request.percentile());
        entity.setAggregation(request.aggregation());
        entity.setValue(request.value());
        entity.setSampleCount(request.sampleCount());
        entity.setCalculatedAt(request.calculatedAt());
        entity.setWindowSeconds(request.windowSeconds());
        AnomalyBaselineEntity saved = repository.save(entity);
        cache(saved);
        return saved;
    }

    String cacheKey(BaselineUpsertRequest request) {
        return "vdt:anomaly:baseline:%s:%s:%s:%s:%s:%d:%s:%d".formatted(
                request.metric(), request.scopeType(), request.scopeKey(), request.timeBucketType(), request.timeBucket(),
                request.historyDays(), request.aggregation(), request.windowSeconds());
    }

    private void validate(BaselineUpsertRequest request) {
        if (request == null || request.metric() == null || request.scopeType() == null || request.scopeKey() == null
                || request.timeBucketType() == null || request.timeBucket() == null || request.aggregation() == null
                || request.value() == null || request.calculatedAt() == null || request.historyDays() <= 0 || request.windowSeconds() <= 0) {
            throw new IllegalArgumentException("Invalid baseline upsert request");
        }
    }

    private void cache(AnomalyBaselineEntity entity) {
        if (!Boolean.TRUE.equals(properties.getCacheEnabled())) {
            return;
        }
        BaselineUpsertRequest request = new BaselineUpsertRequest(entity.getRuleId(), entity.getMetric(), entity.getScopeType(), entity.getScopeKey(),
                entity.getTimeBucketType(), entity.getTimeBucket(), entity.getHistoryDays(), entity.getPercentile(), entity.getAggregation(),
                entity.getValue(), entity.getSampleCount(), entity.getCalculatedAt(), entity.getWindowSeconds());
        try {
            String value = objectMapper.writeValueAsString(Map.of(
                    "value", entity.getValue(),
                    "sampleCount", entity.getSampleCount(),
                    "calculatedAt", entity.getCalculatedAt().toString()));
            int ttl = properties.getCacheTtlSeconds() == null || properties.getCacheTtlSeconds() <= 0 ? 7200 : properties.getCacheTtlSeconds();
            redisTemplate.opsForValue().set(cacheKey(request), value, Duration.ofSeconds(ttl));
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Failed to cache anomaly baseline metric={} scopeType={} scopeKey={}: {}", entity.getMetric(), entity.getScopeType(), entity.getScopeKey(), e.getMessage());
        }
    }
}
