package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.sharedlib.config.VdtShareProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class SecuritySettingsStore {

    private static final TypeReference<List<AccessPolicyRuntimeConfig>> ACCESS_POLICY_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final VdtShareProperties properties;

    public Optional<ExposedApiRuntimeConfig> getExposedApi(UUID endpointId) {
        if (endpointId == null) {
            return Optional.empty();
        }
        return readValue(buildKey(properties.getRuntime().getExposedApiKeyPrefix(), endpointId), ExposedApiRuntimeConfig.class);
    }

    public List<AccessPolicyRuntimeConfig> getAccessPolicies(UUID exposedApiId) {
        if (exposedApiId == null) {
            return List.of();
        }
        return readList(buildKey(properties.getRuntime().getAccessPolicyKeyPrefix(), exposedApiId), ACCESS_POLICY_LIST_TYPE);
    }

    public Optional<ClientCredentialRuntimeConfig> getCredential(String keyId) {
        if (!StringUtils.hasText(keyId)) {
            return Optional.empty();
        }
        return readValue(buildKey(properties.getRuntime().getKeyIdPrefix(), keyId), ClientCredentialRuntimeConfig.class);
    }

    public Optional<ClientPermissionRuntimeConfig> getClientPermission(UUID clientId, UUID exposedApiId) {
        if (clientId == null || exposedApiId == null) {
            return Optional.empty();
        }
        var key = properties.getRuntime().getClientPermissionKeyPrefix() + ":" + clientId + ":" + exposedApiId;
        return readValue(key, ClientPermissionRuntimeConfig.class);
    }

    private <T> Optional<T> readValue(String key, Class<T> type) {
        try {
            var value = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(value)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException e) {
            return handleReadFailure(key, e);
        } catch (RuntimeException e) {
            return handleReadFailure(key, e);
        }
    }

    private <T> List<T> readList(String key, TypeReference<List<T>> type) {
        try {
            var value = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(value)) {
                return List.of();
            }
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            return handleListReadFailure(key, e);
        } catch (RuntimeException e) {
            return handleListReadFailure(key, e);
        }
    }

    private <T> Optional<T> handleReadFailure(String key, Exception e) {
        if (properties.getRuntime().isFailOpen()) {
            log.warn("Failed to read runtime security setting from Redis, failing open: key={}", key, e);
            return Optional.empty();
        }
        throw new IllegalStateException("Failed to read runtime security setting from Redis: " + key, e);
    }

    private <T> List<T> handleListReadFailure(String key, Exception e) {
        if (properties.getRuntime().isFailOpen()) {
            log.warn("Failed to read runtime security setting list from Redis, failing open: key={}", key, e);
            return List.of();
        }
        throw new IllegalStateException("Failed to read runtime security setting list from Redis: " + key, e);
    }

    private String buildKey(String prefix, Object id) {
        return prefix + ":" + id;
    }
}
