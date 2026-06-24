package com.pm.be.service.accesspolicy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.dto.response.accesspolicy.AccessPolicyRedisConfig;
import com.pm.be.entity.accesspolicy.AccessPolicyEntity;
import com.pm.be.repository.accesspolicy.AccessPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessPolicyRedisSyncService {

    private static final String ACCESS_POLICY_KEY_PREFIX = "vdt:access-policy";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AccessPolicyRepository accessPolicyRepo;

    public void syncByExposedApiId(UUID exposedApiId) {
        afterCommit(() -> doSyncByExposedApiId(exposedApiId));
    }

    private void doSyncByExposedApiId(UUID exposedApiId) {
        if (exposedApiId == null) {
            return;
        }

        var key = buildAccessPolicyKey(exposedApiId);
        List<AccessPolicyRedisConfig> configs = accessPolicyRepo.findByExposedApiId(exposedApiId).stream()
                .map(this::toRedisConfig)
                .toList();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(configs));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize access_policy Redis config: exposedApiId={}", exposedApiId, e);
        } catch (RuntimeException e) {
            log.warn("Failed to sync access_policy Redis config: exposedApiId={}", exposedApiId, e);
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

    private String buildAccessPolicyKey(UUID exposedApiId) {
        return ACCESS_POLICY_KEY_PREFIX + ":" + exposedApiId;
    }

    private AccessPolicyRedisConfig toRedisConfig(AccessPolicyEntity entity) {
        return AccessPolicyRedisConfig.builder()
                .id(entity.getId())
                .exposedApiId(entity.getExposedApiId())
                .type(entity.getType())
                .matchType(entity.getMatchType())
                .matchValue(entity.getMatchValue())
                .temporary(entity.getTemporary())
                .expiresAt(entity.getExpiresAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
