package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyDetectorProperties;
import com.pm.be.enums.AnomalyScopeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RedisMetricReader {
    private final StringRedisTemplate redisTemplate;
    private final AnomalyDetectorProperties properties;

    public BigDecimal readCounter(int windowSeconds, long windowStart, AnomalyScopeType scopeType, String scopeKey, String metric) {
        String value = redisTemplate.opsForValue().get(buildCounterKey(windowSeconds, windowStart, scopeType, scopeKey, metric));
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim());
    }

    public long windowStart(Instant timestamp, int windowSeconds) {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
        long epochSecond = timestamp.getEpochSecond();
        return epochSecond - (epochSecond % windowSeconds);
    }

    public String buildCounterKey(int windowSeconds, long windowStart, AnomalyScopeType scopeType, String scopeKey, String metric) {
        return keyPrefix()
                + ":counter:"
                + windowSeconds
                + ":"
                + windowStart
                + ":"
                + scopeType.name()
                + ":"
                + scopeKey
                + ":"
                + metric;
    }

    private String keyPrefix() {
        return StringUtils.hasText(properties.getKeyPrefix()) ? properties.getKeyPrefix().trim() : "vdt:anomaly";
    }
}
