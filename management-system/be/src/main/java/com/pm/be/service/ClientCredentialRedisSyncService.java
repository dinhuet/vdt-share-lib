package com.pm.be.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.dto.response.ClientCredentialRedisConfig;
import com.pm.be.entity.ClientCredentialEntity;
import com.pm.be.entity.ClientEntity;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.repository.ClientRepository;
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
public class ClientCredentialRedisSyncService {

    private static final String KEY_ID_PREFIX = "vdt:key-id";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ClientRepository clientRepo;
    private final MicroServiceRepository microServiceRepo;

    public void syncCredential(ClientCredentialEntity credential) {
        afterCommit(() -> doSyncCredential(credential));
    }

    private void doSyncCredential(ClientCredentialEntity credential) {
        if (credential == null || credential.getKeyId() == null) {
            return;
        }

        var key = buildCredentialKey(credential.getKeyId());
        var client = clientRepo.findById(credential.getClientId()).orElse(null);
        var microService = microServiceRepo.findById(credential.getMicroServiceId()).orElse(null);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(toRedisConfig(credential, client, microService)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize client_credential Redis config: credentialId={}", credential.getId(), e);
        } catch (RuntimeException e) {
            log.warn("Failed to sync client_credential Redis config: credentialId={}", credential.getId(), e);
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

    private String buildCredentialKey(String keyId) {
        return KEY_ID_PREFIX + ":" + keyId;
    }

    private ClientCredentialRedisConfig toRedisConfig(
            ClientCredentialEntity credential,
            ClientEntity client,
            MicroServiceEntity microService) {
        return ClientCredentialRedisConfig.builder()
                .id(credential.getId())
                .clientId(credential.getClientId())
                .clientCode(client != null ? client.getClientCode() : null)
                .clientName(client != null ? client.getName() : null)
                .microServiceId(credential.getMicroServiceId())
                .microServiceName(microService != null ? microService.getName() : null)
                .keyId(credential.getKeyId())
                .apiKeyHash(credential.getApiKeyHash())
                .signingSecretEncrypted(credential.getSigningSecretEncrypted())
                .algorithm(credential.getAlgorithm())
                .status(credential.getStatus())
                .expiresAt(credential.getExpiresAt())
                .revokedAt(credential.getRevokedAt())
                .revokedBy(credential.getRevokedBy())
                .revokeReason(credential.getRevokeReason())
                .createdAt(credential.getCreatedAt())
                .updatedAt(credential.getUpdatedAt())
                .build();
    }
}
