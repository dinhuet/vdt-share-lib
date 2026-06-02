package com.pm.be.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.dto.response.ExposedApiRedisConfig;
import com.pm.be.entity.ExposedApiEntity;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.enums.SyncStatus;
import com.pm.be.repository.MicroServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExposedApiRedisSyncService {

    private static final String EXPOSED_API_KEY_PREFIX = "vdt:exposed-api";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MicroServiceRepository microServiceRepo;

    public void syncApi(ExposedApiEntity api) {
        afterCommit(() -> doSyncApi(api));
    }

    public void syncAll(Collection<ExposedApiEntity> apis) {
        for (var api : apis) {
            syncApi(api);
        }
    }

    public void deleteApi(ExposedApiEntity api) {
        afterCommit(() -> doDeleteApi(api));
    }

    private void doSyncApi(ExposedApiEntity api) {
        var service = microServiceRepo.findById(api.getMicroServiceId()).orElse(null);
        if (service == null) {
            log.warn("Skip exposed_api Redis sync because micro_service was not found: apiId={}", api.getId());
            return;
        }

        if (api.getSyncStatus() != SyncStatus.ACTIVE) {
            deleteApi(api, service);
            return;
        }

        var key = buildExposedApiKey(service.getName(), api.getName());
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(toRedisConfig(api, service)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize exposed_api Redis config: apiId={}", api.getId(), e);
        } catch (RuntimeException e) {
            log.warn("Failed to sync exposed_api Redis config: apiId={}", api.getId(), e);
        }
    }

    private void doDeleteApi(ExposedApiEntity api) {
        var service = microServiceRepo.findById(api.getMicroServiceId()).orElse(null);
        if (service == null) {
            log.warn("Skip exposed_api Redis delete because micro_service was not found: apiId={}", api.getId());
            return;
        }
        deleteApi(api, service);
    }

    private void deleteApi(ExposedApiEntity api, MicroServiceEntity service) {
        var key = buildExposedApiKey(service.getName(), api.getName());
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("Failed to delete exposed_api Redis config: apiId={}", api.getId(), e);
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private String buildExposedApiKey(String serviceName, String apiName) {
        return EXPOSED_API_KEY_PREFIX + ":" + serviceName + ":" + apiName;
    }

    private ExposedApiRedisConfig toRedisConfig(ExposedApiEntity api, MicroServiceEntity service) {
        return ExposedApiRedisConfig.builder()
                .id(api.getId())
                .microServiceId(api.getMicroServiceId())
                .serviceName(service.getName())
                .apiName(api.getName())
                .path(api.getPath())
                .method(api.getMethod())
                .protocol(api.getProtocol())
                .maxRequests(api.getMaxRequests())
                .throttleWindowSec(api.getThrottleWindowSec())
                .maxRequestKb(api.getMaxRequestKb())
                .maxResponseKb(api.getMaxResponseKb())
                .latencyThresholdMs(api.getLatencyThresholdMs())
                .timeoutMs(api.getTimeoutMs())
                .logRetentionDays(api.getLogRetentionDays())
                .useDefaultConfig(api.getUseDefaultConfig())
                .notificationRuleId(api.getNotificationRuleId())
                .enabled(api.getEnabled())
                .syncStatus(api.getSyncStatus())
                .lastSyncedAt(api.getLastSyncedAt())
                .updatedAt(api.getUpdatedAt())
                .build();
    }
}
