package com.pm.be.service.anomaly;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.config.BaselineRuleProperties;
import com.pm.be.entity.anomaly.AnomalyBaselineEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;
import com.pm.be.repository.anomaly.AnomalyBaselineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaselineLookupService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final AnomalyBaselineRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final BaselineRuleProperties properties;

    public Optional<AnomalyBaselineEntity> lookup(String metric, AnomalyScopeType scopeType, String scopeKey,
                                                  AnomalyTimeBucketType timeBucketType, String timeBucket,
                                                  int historyDays, String aggregation, int windowSeconds) {
        if (!StringUtils.hasText(metric) || scopeType == null || !StringUtils.hasText(scopeKey)
                || timeBucketType == null || !StringUtils.hasText(timeBucket) || !StringUtils.hasText(aggregation)) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(properties.getBaselineCacheEnabled())) {
            Optional<AnomalyBaselineEntity> cached = readCache(metric, scopeType, scopeKey, timeBucketType, timeBucket, historyDays, aggregation, windowSeconds);
            if (cached.isPresent()) {
                return cached;
            }
        }
        try {
            return repository.findByMetricAndScopeTypeAndScopeKeyAndTimeBucketTypeAndTimeBucketAndHistoryDaysAndAggregationAndWindowSeconds(
                    metric, scopeType, scopeKey, timeBucketType, timeBucket, historyDays, aggregation, windowSeconds);
        } catch (RuntimeException e) {
            log.warn("Failed to load anomaly baseline from DB metric={} scopeType={} scopeKey={} timeBucket={}",
                    metric, scopeType, scopeKey, timeBucket, e);
            return Optional.empty();
        }
    }

    private Optional<AnomalyBaselineEntity> readCache(String metric, AnomalyScopeType scopeType, String scopeKey,
                                                      AnomalyTimeBucketType timeBucketType, String timeBucket,
                                                      int historyDays, String aggregation, int windowSeconds) {
        String key = cacheKey(metric, scopeType, scopeKey, timeBucketType, timeBucket, historyDays, aggregation, windowSeconds);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(value)) {
                return Optional.empty();
            }
            Map<String, Object> payload = objectMapper.readValue(value, MAP_TYPE);
            AnomalyBaselineEntity entity = new AnomalyBaselineEntity();
            entity.setMetric(metric);
            entity.setScopeType(scopeType);
            entity.setScopeKey(scopeKey);
            entity.setTimeBucketType(timeBucketType);
            entity.setTimeBucket(timeBucket);
            entity.setHistoryDays(historyDays);
            entity.setAggregation(aggregation);
            entity.setWindowSeconds(windowSeconds);
            entity.setValue(new BigDecimal(String.valueOf(payload.get("value"))));
            entity.setSampleCount(Long.parseLong(String.valueOf(payload.get("sampleCount"))));
            entity.setCalculatedAt(LocalDateTime.parse(String.valueOf(payload.get("calculatedAt"))));
            return Optional.of(entity);
        } catch (RuntimeException e) {
            log.warn("Failed to read cached anomaly baseline key={}", key, e);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to parse cached anomaly baseline key={}", key, e);
            return Optional.empty();
        }
    }

    private String cacheKey(String metric, AnomalyScopeType scopeType, String scopeKey, AnomalyTimeBucketType timeBucketType,
                            String timeBucket, int historyDays, String aggregation, int windowSeconds) {
        return "vdt:anomaly:baseline:%s:%s:%s:%s:%s:%d:%s:%d".formatted(
                metric, scopeType, scopeKey, timeBucketType, timeBucket, historyDays, aggregation, windowSeconds);
    }
}
