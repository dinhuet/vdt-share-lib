package com.pm.be.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.dto.response.ClientPermissionRedisConfig;
import com.pm.be.entity.ClientEntity;
import com.pm.be.entity.ClientExposedApiPermissionEntity;
import com.pm.be.entity.ExposedApiEntity;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.repository.ClientRepository;
import com.pm.be.repository.ExposedApiRepository;
import com.pm.be.repository.MicroServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientPermissionRedisSyncService {

    private static final String CLIENT_PERMISSION_KEY_PREFIX = "vdt:client-permission";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ClientRepository clientRepo;
    private final ExposedApiRepository exposedApiRepo;
    private final MicroServiceRepository microServiceRepo;

    public void syncPermission(ClientExposedApiPermissionEntity permission) {
        afterCommit(() -> doSyncPermission(permission));
    }

    public void deletePermission(ClientExposedApiPermissionEntity permission) {
        afterCommit(() -> doDeletePermission(permission));
    }

    private void doSyncPermission(ClientExposedApiPermissionEntity permission) {
        if (permission == null || permission.getClientId() == null || permission.getExposedApiId() == null) {
            return;
        }

        var key = buildPermissionKey(permission);
        var client = clientRepo.findById(permission.getClientId()).orElse(null);
        var exposedApi = exposedApiRepo.findById(permission.getExposedApiId()).orElse(null);
        var microService = exposedApi != null ? microServiceRepo.findById(exposedApi.getMicroServiceId()).orElse(null) : null;
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(toRedisConfig(permission, client, exposedApi, microService)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize client_permission Redis config: permissionId={}", permission.getId(), e);
        } catch (RuntimeException e) {
            log.warn("Failed to sync client_permission Redis config: permissionId={}", permission.getId(), e);
        }
    }

    private void doDeletePermission(ClientExposedApiPermissionEntity permission) {
        if (permission == null || permission.getClientId() == null || permission.getExposedApiId() == null) {
            return;
        }
        var key = buildPermissionKey(permission);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("Failed to delete client_permission Redis config: permissionId={}", permission.getId(), e);
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

    private String buildPermissionKey(ClientExposedApiPermissionEntity permission) {
        return CLIENT_PERMISSION_KEY_PREFIX + ":" + permission.getClientId() + ":" + permission.getExposedApiId();
    }

    private ClientPermissionRedisConfig toRedisConfig(
            ClientExposedApiPermissionEntity permission,
            ClientEntity client,
            ExposedApiEntity exposedApi,
            MicroServiceEntity microService) {
        return ClientPermissionRedisConfig.builder()
                .id(permission.getId())
                .clientId(permission.getClientId())
                .clientCode(client != null ? client.getClientCode() : null)
                .clientName(client != null ? client.getName() : null)
                .exposedApiId(permission.getExposedApiId())
                .exposedApiName(exposedApi != null ? exposedApi.getName() : null)
                .microServiceId(exposedApi != null ? exposedApi.getMicroServiceId() : null)
                .microServiceName(microService != null ? microService.getName() : null)
                .method(exposedApi != null ? exposedApi.getMethod() : null)
                .path(exposedApi != null ? exposedApi.getPath() : null)
                .protocol(exposedApi != null ? exposedApi.getProtocol() : null)
                .enabled(permission.getEnabled())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}
