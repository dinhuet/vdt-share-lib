package com.pm.be.service;

import com.pm.be.config.ExposedApiDefaultProperties;
import com.pm.be.entity.ApiDefaultConfigEntity;
import com.pm.be.entity.ExposedApiEntity;
import com.pm.be.enums.DefaultConfigScope;
import com.pm.be.repository.ApiDefaultConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiDefaultConfigResolver {

    private final ApiDefaultConfigRepository apiDefaultConfigRepo;
    private final ExposedApiDefaultProperties exposedApiDefaults;

    public ResolvedApiDefaultConfig resolve(UUID microServiceId) {
        var serviceDefault = apiDefaultConfigRepo
                .findByScopeAndMicroServiceId(DefaultConfigScope.SERVICE, microServiceId)
                .orElse(null);
        var globalDefault = apiDefaultConfigRepo
                .findByScopeAndMicroServiceIdIsNull(DefaultConfigScope.GLOBAL)
                .orElse(null);

        return new ResolvedApiDefaultConfig(
                firstNonNull(valueOf(serviceDefault, Field.MAX_REQUESTS), valueOf(globalDefault, Field.MAX_REQUESTS), exposedApiDefaults.getMaxRequests()),
                firstNonNull(valueOf(serviceDefault, Field.THROTTLE_WINDOW_SEC), valueOf(globalDefault, Field.THROTTLE_WINDOW_SEC), exposedApiDefaults.getThrottleWindowSec()),
                firstNonNull(valueOf(serviceDefault, Field.MAX_REQUEST_KB), valueOf(globalDefault, Field.MAX_REQUEST_KB), exposedApiDefaults.getMaxRequestKb()),
                firstNonNull(valueOf(serviceDefault, Field.MAX_RESPONSE_KB), valueOf(globalDefault, Field.MAX_RESPONSE_KB), exposedApiDefaults.getMaxResponseKb()),
                firstNonNull(valueOf(serviceDefault, Field.LATENCY_THRESHOLD_MS), valueOf(globalDefault, Field.LATENCY_THRESHOLD_MS), exposedApiDefaults.getLatencyThresholdMs()),
                firstNonNull(valueOf(serviceDefault, Field.TIMEOUT_MS), valueOf(globalDefault, Field.TIMEOUT_MS), exposedApiDefaults.getTimeoutMs()),
                firstNonNull(valueOf(serviceDefault, Field.LOG_RETENTION_DAYS), valueOf(globalDefault, Field.LOG_RETENTION_DAYS), exposedApiDefaults.getLogRetentionDays()),
                firstNonNull(valueOf(serviceDefault, Field.ENABLED), valueOf(globalDefault, Field.ENABLED), exposedApiDefaults.getEnabled())
        );
    }

    public void applyTo(ExposedApiEntity entity) {
        var config = resolve(entity.getMicroServiceId());
        entity.setMaxRequests(config.maxRequests());
        entity.setThrottleWindowSec(config.throttleWindowSec());
        entity.setMaxRequestKb(config.maxRequestKb());
        entity.setMaxResponseKb(config.maxResponseKb());
        entity.setLatencyThresholdMs(config.latencyThresholdMs());
        entity.setTimeoutMs(config.timeoutMs());
        entity.setLogRetentionDays(config.logRetentionDays());
        entity.setEnabled(config.enabled());
    }

    private Object valueOf(ApiDefaultConfigEntity entity, Field field) {
        if (entity == null) {
            return null;
        }
        return switch (field) {
            case MAX_REQUESTS -> entity.getMaxRequests();
            case THROTTLE_WINDOW_SEC -> entity.getThrottleWindowSec();
            case MAX_REQUEST_KB -> entity.getMaxRequestKb();
            case MAX_RESPONSE_KB -> entity.getMaxResponseKb();
            case LATENCY_THRESHOLD_MS -> entity.getLatencyThresholdMs();
            case TIMEOUT_MS -> entity.getTimeoutMs();
            case LOG_RETENTION_DAYS -> entity.getLogRetentionDays();
            case ENABLED -> entity.getEnabled();
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T firstNonNull(Object first, Object second, T fallback) {
        if (first != null) {
            return (T) first;
        }
        if (second != null) {
            return (T) second;
        }
        return fallback;
    }

    private enum Field {
        MAX_REQUESTS,
        THROTTLE_WINDOW_SEC,
        MAX_REQUEST_KB,
        MAX_RESPONSE_KB,
        LATENCY_THRESHOLD_MS,
        TIMEOUT_MS,
        LOG_RETENTION_DAYS,
        ENABLED
    }

    public record ResolvedApiDefaultConfig(
            Integer maxRequests,
            Integer throttleWindowSec,
            Integer maxRequestKb,
            Integer maxResponseKb,
            Integer latencyThresholdMs,
            Integer timeoutMs,
            Integer logRetentionDays,
            Boolean enabled
    ) {}
}
