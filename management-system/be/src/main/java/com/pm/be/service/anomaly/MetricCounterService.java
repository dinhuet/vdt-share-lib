package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyDetectorProperties;
import com.pm.be.dto.anomaly.DistinctDeniedEndpointIncrement;
import com.pm.be.dto.anomaly.MetricExtractionResult;
import com.pm.be.dto.anomaly.MetricIncrement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricCounterService {
    private final StringRedisTemplate redisTemplate;
    private final AnomalyDetectorProperties properties;

    public void increment(MetricExtractionResult result) {
        if (result == null) {
            return;
        }
        incrementCounters(result.metricIncrements());
        incrementDistinctDeniedEndpointSets(result.distinctDeniedEndpointIncrements());
    }

    public void incrementCounters(List<MetricIncrement> increments) {
        if (increments == null || increments.isEmpty()) {
            return;
        }
        for (MetricIncrement increment : increments) {
            if (!isValid(increment)) {
                log.warn("Skip invalid metric increment: metric={} scopeType={} scopeKey={}",
                        increment == null ? null : increment.metric(),
                        increment == null ? null : increment.scopeType(),
                        increment == null ? null : increment.scopeKey());
                continue;
            }
            String key = buildCounterKey(increment);
            try {
                redisTemplate.opsForValue().increment(key, increment.amount());
                redisTemplate.expire(key, Duration.ofSeconds(increment.ttlSeconds()));
            } catch (RuntimeException e) {
                log.warn("Failed to increment anomaly Redis counter: key={}", key, e);
            }
        }
    }

    public void incrementDistinctDeniedEndpointSets(List<DistinctDeniedEndpointIncrement> increments) {
        if (increments == null || increments.isEmpty()) {
            return;
        }
        for (DistinctDeniedEndpointIncrement increment : increments) {
            if (!isValid(increment)) {
                log.warn("Skip invalid distinct denied endpoint increment: identityType={} identityValue={} endpointId={}",
                        increment == null ? null : increment.identityType(),
                        increment == null ? null : increment.identityValue(),
                        increment == null ? null : increment.endpointId());
                continue;
            }
            String key = buildDistinctDeniedEndpointSetKey(increment);
            try {
                redisTemplate.opsForSet().add(key, increment.endpointId());
                redisTemplate.expire(key, Duration.ofSeconds(increment.ttlSeconds()));
            } catch (RuntimeException e) {
                log.warn("Failed to increment anomaly Redis distinct denied endpoint set: key={}", key, e);
            }
        }
    }

    public String buildCounterKey(MetricIncrement increment) {
        long windowStart = windowStart(increment.eventTimestamp().getEpochSecond(), increment.windowSeconds());
        return keyPrefix()
                + ":counter:"
                + increment.windowSeconds()
                + ":"
                + windowStart
                + ":"
                + increment.scopeType().name()
                + ":"
                + increment.scopeKey()
                + ":"
                + increment.metric();
    }

    public String buildDistinctDeniedEndpointSetKey(DistinctDeniedEndpointIncrement increment) {
        long windowStart = windowStart(increment.eventTimestamp().getEpochSecond(), increment.windowSeconds());
        return keyPrefix()
                + ":set:"
                + increment.windowSeconds()
                + ":"
                + windowStart
                + ":"
                + increment.identityType()
                + ":"
                + increment.identityValue()
                + ":distinct_denied_endpoint";
    }

    public long windowStart(long epochSecond, int windowSeconds) {
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
        return epochSecond - (epochSecond % windowSeconds);
    }

    private boolean isValid(MetricIncrement increment) {
        return increment != null
                && StringUtils.hasText(increment.metric())
                && increment.amount() > 0
                && increment.windowSeconds() > 0
                && increment.scopeType() != null
                && StringUtils.hasText(increment.scopeKey())
                && increment.eventTimestamp() != null
                && increment.ttlSeconds() > 0;
    }

    private boolean isValid(DistinctDeniedEndpointIncrement increment) {
        return increment != null
                && increment.windowSeconds() > 0
                && StringUtils.hasText(increment.identityType())
                && StringUtils.hasText(increment.identityValue())
                && StringUtils.hasText(increment.endpointId())
                && increment.eventTimestamp() != null
                && increment.ttlSeconds() > 0;
    }

    private String keyPrefix() {
        return StringUtils.hasText(properties.getKeyPrefix()) ? properties.getKeyPrefix().trim() : "vdt:anomaly";
    }
}
