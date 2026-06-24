package com.pm.be.service.anomaly;

import com.pm.be.dto.anomaly.CurrentMetricValue;
import com.pm.be.enums.AnomalyScopeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentMetricReader {
    private static final MathContext RATE_CONTEXT = MathContext.DECIMAL64;
    private static final Map<String, String> RATE_NUMERATORS = Map.of(
            "error_rate_5m", "failed_count",
            "denied_rate_5m", "denied_count",
            "timeout_rate_5m", "timeout_count",
            "retry_rate_5m", "retry_count",
            "auth_fail_rate_5m", "auth_fail_count",
            "slow_request_rate_5m", "slow_request_count"
    );

    private final RedisMetricReader redisMetricReader;

    public Optional<CurrentMetricValue> read(String metric, int windowSeconds, AnomalyScopeType scopeType, String scopeKey,
                                             Instant timestamp, int minSampleCount) {
        long windowStart = redisMetricReader.windowStart(timestamp, windowSeconds);
        return readAtWindow(metric, windowSeconds, windowStart, scopeType, scopeKey, minSampleCount);
    }

    public Optional<CurrentMetricValue> readAtWindow(String metric, int windowSeconds, long windowStart, AnomalyScopeType scopeType,
                                                     String scopeKey, int minSampleCount) {
        try {
            String numeratorMetric = RATE_NUMERATORS.get(metric);
            if (numeratorMetric != null) {
                return readRate(metric, numeratorMetric, windowSeconds, windowStart, scopeType, scopeKey, minSampleCount);
            }
            BigDecimal current = redisMetricReader.readCounter(windowSeconds, windowStart, scopeType, scopeKey, counterMetric(metric));
            return Optional.of(new CurrentMetricValue(metric, current, current, null, false));
        } catch (RuntimeException e) {
            log.warn("Failed to read current anomaly metric metric={} scopeType={} scopeKey={} windowSeconds={}",
                    metric, scopeType, scopeKey, windowSeconds, e);
            return Optional.empty();
        }
    }

    public long windowStart(Instant timestamp, int windowSeconds) {
        return redisMetricReader.windowStart(timestamp, windowSeconds);
    }

    private Optional<CurrentMetricValue> readRate(String metric, String numeratorMetric, int windowSeconds, long windowStart,
                                                  AnomalyScopeType scopeType, String scopeKey, int minSampleCount) {
        BigDecimal numerator = redisMetricReader.readCounter(windowSeconds, windowStart, scopeType, scopeKey, numeratorMetric);
        BigDecimal denominator = redisMetricReader.readCounter(windowSeconds, windowStart, scopeType, scopeKey, "request_count");
        if (denominator.compareTo(BigDecimal.ZERO) <= 0 || denominator.compareTo(BigDecimal.valueOf(minSampleCount)) < 0) {
            log.debug("Skip rate metric because denominator is below min sample metric={} denominator={} minSampleCount={}",
                    metric, denominator, minSampleCount);
            return Optional.empty();
        }
        BigDecimal rate = numerator.divide(denominator, RATE_CONTEXT);
        return Optional.of(new CurrentMetricValue(metric, rate, numerator, denominator, true));
    }

    private String counterMetric(String metric) {
        if (metric == null) {
            return null;
        }
        if (metric.endsWith("_1m") || metric.endsWith("_5m")) {
            return metric.substring(0, metric.length() - 3);
        }
        return metric;
    }
}
