package com.pm.be.service;

import com.pm.be.config.ExposedApiDefaultProperties;
import com.pm.be.entity.ApiDefaultConfigEntity;
import com.pm.be.entity.ClientApiEntity;
import com.pm.be.entity.ExposedApiEntity;
import com.pm.be.enums.ApiConfigType;
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
        return resolveExposed(microServiceId);
    }

    public ResolvedApiDefaultConfig resolveExposed(UUID microServiceId) {
        var serviceDefault = findDefault(ApiConfigType.EXPOSED, DefaultConfigScope.SERVICE, microServiceId);
        var globalDefault = findGlobalDefault(ApiConfigType.EXPOSED);

        return new ResolvedApiDefaultConfig(
                firstNonNull(valueOf(serviceDefault, Field.MAX_REQUESTS), valueOf(globalDefault, Field.MAX_REQUESTS), exposedApiDefaults.getMaxRequests()),
                firstNonNull(valueOf(serviceDefault, Field.THROTTLE_WINDOW_SEC), valueOf(globalDefault, Field.THROTTLE_WINDOW_SEC), exposedApiDefaults.getThrottleWindowSec()),
                firstNonNull(valueOf(serviceDefault, Field.MAX_REQUEST_KB), valueOf(globalDefault, Field.MAX_REQUEST_KB), exposedApiDefaults.getMaxRequestKb()),
                firstNonNull(valueOf(serviceDefault, Field.MAX_RESPONSE_KB), valueOf(globalDefault, Field.MAX_RESPONSE_KB), exposedApiDefaults.getMaxResponseKb()),
                firstNonNull(valueOf(serviceDefault, Field.LATENCY_THRESHOLD_MS), valueOf(globalDefault, Field.LATENCY_THRESHOLD_MS), exposedApiDefaults.getLatencyThresholdMs()),
                firstNonNull(valueOf(serviceDefault, Field.TIMEOUT_MS), valueOf(globalDefault, Field.TIMEOUT_MS), exposedApiDefaults.getTimeoutMs()),
                firstNonNull(valueOf(serviceDefault, Field.LOG_RETENTION_DAYS), valueOf(globalDefault, Field.LOG_RETENTION_DAYS), exposedApiDefaults.getLogRetentionDays()),
                null,
                null,
                null,
                firstNonNull(valueOf(serviceDefault, Field.NOTIFICATION_RULE_ID), valueOf(globalDefault, Field.NOTIFICATION_RULE_ID), null),
                firstNonNull(valueOf(serviceDefault, Field.ENABLED), valueOf(globalDefault, Field.ENABLED), exposedApiDefaults.getEnabled())
        );
    }

    public ResolvedApiDefaultConfig resolveClient(UUID microServiceId) {
        var serviceDefault = findDefault(ApiConfigType.CLIENT, DefaultConfigScope.SERVICE, microServiceId);
        var globalDefault = findGlobalDefault(ApiConfigType.CLIENT);

        return new ResolvedApiDefaultConfig(
                null,
                null,
                null,
                null,
                firstNonNull(valueOf(serviceDefault, Field.LATENCY_THRESHOLD_MS), valueOf(globalDefault, Field.LATENCY_THRESHOLD_MS), null),
                firstNonNull(valueOf(serviceDefault, Field.TIMEOUT_MS), valueOf(globalDefault, Field.TIMEOUT_MS), 30000),
                firstNonNull(valueOf(serviceDefault, Field.LOG_RETENTION_DAYS), valueOf(globalDefault, Field.LOG_RETENTION_DAYS), 30),
                firstNonNull(valueOf(serviceDefault, Field.MAX_RETRIES), valueOf(globalDefault, Field.MAX_RETRIES), 3),
                firstNonNull(valueOf(serviceDefault, Field.RETRY_DELAY_MS), valueOf(globalDefault, Field.RETRY_DELAY_MS), 1000),
                firstNonNull(valueOf(serviceDefault, Field.FAILURE_ACTION), valueOf(globalDefault, Field.FAILURE_ACTION), null),
                firstNonNull(valueOf(serviceDefault, Field.NOTIFICATION_RULE_ID), valueOf(globalDefault, Field.NOTIFICATION_RULE_ID), null),
                firstNonNull(valueOf(serviceDefault, Field.ENABLED), valueOf(globalDefault, Field.ENABLED), true)
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
        entity.setNotificationRuleId(config.notificationRuleId());
        entity.setEnabled(config.enabled());
    }

    public void applyTo(ClientApiEntity entity) {
        var config = resolveClient(entity.getMicroServiceId());
        entity.setLatencyThresholdMs(config.latencyThresholdMs());
        entity.setTimeoutMs(config.timeoutMs());
        entity.setMaxRetries(config.maxRetries());
        entity.setRetryDelayMs(config.retryDelayMs());
        entity.setFailureAction(config.failureAction());
        entity.setLogRetentionDays(config.logRetentionDays());
        entity.setNotificationRuleId(config.notificationRuleId());
        entity.setEnabled(config.enabled());
    }

    private ApiDefaultConfigEntity findDefault(ApiConfigType apiType, DefaultConfigScope scope, UUID microServiceId) {
        return apiDefaultConfigRepo.findByApiTypeAndScopeAndMicroServiceId(apiType, scope, microServiceId)
                .orElse(null);
    }

    private ApiDefaultConfigEntity findGlobalDefault(ApiConfigType apiType) {
        return apiDefaultConfigRepo.findByApiTypeAndScopeAndMicroServiceIdIsNull(apiType, DefaultConfigScope.GLOBAL)
                .orElse(null);
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
            case MAX_RETRIES -> entity.getMaxRetries();
            case RETRY_DELAY_MS -> entity.getRetryDelayMs();
            case FAILURE_ACTION -> entity.getFailureAction();
            case NOTIFICATION_RULE_ID -> entity.getNotificationRuleId();
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
        MAX_RETRIES,
        RETRY_DELAY_MS,
        FAILURE_ACTION,
        NOTIFICATION_RULE_ID,
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
            Integer maxRetries,
            Integer retryDelayMs,
            String failureAction,
            UUID notificationRuleId,
            Boolean enabled
    ) {}
}
