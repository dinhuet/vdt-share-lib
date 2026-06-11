package com.pm.sharedlib.runtime;

import com.pm.sharedlib.config.VdtShareProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final VdtShareProperties properties;

    public RateLimitResult check(ExposedApiRuntimeConfig config, String identityType, String identityValue) {
        if (config.getMaxRequests() == null || config.getThrottleWindowSec() == null
                || config.getMaxRequests() <= 0 || config.getThrottleWindowSec() <= 0) {
            return new RateLimitResult(true, 0, 0, 0);
        }

        var safeIdentityType = StringUtils.hasText(identityType) ? identityType : "anonymous";
        var safeIdentityValue = StringUtils.hasText(identityValue) ? identityValue : "unknown";
        var window = Instant.now().getEpochSecond() / config.getThrottleWindowSec();
        var key = buildKey(config.getEndpointId(), safeIdentityType, safeIdentityValue, window);

        try {
            Long current = redisTemplate.opsForValue().increment(key);
            if (current != null && current == 1L) {
                redisTemplate.expire(key, config.getThrottleWindowSec(), TimeUnit.SECONDS);
            }
            long currentRequests = current == null ? 0 : current;
            return new RateLimitResult(
                    currentRequests <= config.getMaxRequests(),
                    currentRequests,
                    config.getMaxRequests(),
                    config.getThrottleWindowSec());
        } catch (RuntimeException e) {
            if (properties.getRuntime().isFailOpen()) {
                return new RateLimitResult(true, 0, config.getMaxRequests(), config.getThrottleWindowSec());
            }
            throw new IllegalStateException("Failed to check rate limit in Redis: " + key, e);
        }
    }

    private String buildKey(UUID endpointId, String identityType, String identityValue, long window) {
        return properties.getRuntime().getRateLimitKeyPrefix()
                + ":" + endpointId
                + ":" + identityType
                + ":" + identityValue
                + ":" + window;
    }
}
