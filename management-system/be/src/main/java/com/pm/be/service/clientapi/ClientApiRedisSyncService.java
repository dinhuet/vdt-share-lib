package com.pm.be.service.clientapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.dto.response.clientapi.ClientApiRedisConfig;
import com.pm.be.entity.clientapi.ClientApiEntity;
import com.pm.be.entity.microservice.MicroServiceEntity;
import com.pm.be.enums.SyncStatus;
import com.pm.be.repository.microservice.MicroServiceRepository;
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
public class ClientApiRedisSyncService {

    private static final String CLIENT_API_KEY_PREFIX = "vdt:client-api";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MicroServiceRepository microServiceRepo;

    public void syncApi(ClientApiEntity api) {
        afterCommit(() -> doSyncApi(api));
    }

    public void syncAll(Collection<ClientApiEntity> apis) {
        for (var api : apis) {
            syncApi(api);
        }
    }

    public void deleteApi(ClientApiEntity api) {
        afterCommit(() -> doDeleteApi(api));
    }

    private void doSyncApi(ClientApiEntity api) {
        var service = microServiceRepo.findById(api.getMicroServiceId()).orElse(null);
        if (service == null) {
            log.warn("Skip client_api Redis sync because micro_service was not found: apiId={}", api.getId());
            return;
        }

        if (api.getSyncStatus() != SyncStatus.ACTIVE) {
            deleteApi(api, service);
            return;
        }

        var key = buildClientApiKey(api);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(toRedisConfig(api, service)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize client_api Redis config: apiId={}", api.getId(), e);
        } catch (RuntimeException e) {
            log.warn("Failed to sync client_api Redis config: apiId={}", api.getId(), e);
        }
    }

    private void doDeleteApi(ClientApiEntity api) {
        var service = microServiceRepo.findById(api.getMicroServiceId()).orElse(null);
        if (service == null) {
            log.warn("Skip client_api Redis delete because micro_service was not found: apiId={}", api.getId());
            return;
        }
        deleteApi(api, service);
    }

    private void deleteApi(ClientApiEntity api, MicroServiceEntity service) {
        deleteKey(buildClientApiKey(api));
    }

    private void deleteKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("Failed to delete client_api Redis config: key={}", key, e);
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

    private String buildClientApiKey(ClientApiEntity api) {
        return CLIENT_API_KEY_PREFIX + ":" + api.getEndpointId();
    }

    private ClientApiRedisConfig toRedisConfig(ClientApiEntity api, MicroServiceEntity service) {
        return ClientApiRedisConfig.builder()
                .id(api.getId())
                .microServiceId(api.getMicroServiceId())
                .serviceName(service.getName())
                .endpointId(api.getEndpointId())
                .endpointKey(api.getEndpointKey())
                .clientId(api.getClientId())
                .apiName(api.getName())
                .destinationUrl(api.getDestinationUrl())
                .topic(api.getTopic())
                .method(api.getMethod())
                .protocol(api.getProtocol())
                .latencyThresholdMs(api.getLatencyThresholdMs())
                .timeoutMs(api.getTimeoutMs())
                .maxRetries(api.getMaxRetries())
                .retryDelayMs(api.getRetryDelayMs())
                .failureAction(api.getFailureAction())
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
