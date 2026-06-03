package com.pm.be.service;

import com.pm.be.dto.request.ApiDefaultConfigUpsertRequest;
import com.pm.be.dto.response.ApiDefaultConfigResponse;
import com.pm.be.entity.ApiDefaultConfigEntity;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.enums.ApiConfigType;
import com.pm.be.enums.DefaultApplyMode;
import com.pm.be.enums.DefaultConfigScope;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.ApiDefaultConfigRepository;
import com.pm.be.repository.ClientApiRepository;
import com.pm.be.repository.ExposedApiRepository;
import com.pm.be.repository.MicroServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApiDefaultConfigService {

    private final ApiDefaultConfigRepository apiDefaultConfigRepo;
    private final MicroServiceRepository microServiceRepo;
    private final ExposedApiRepository exposedApiRepo;
    private final ClientApiRepository clientApiRepo;
    private final ApiDefaultConfigResolver apiDefaultConfigResolver;
    private final ExposedApiRedisSyncService exposedApiRedisSyncService;
    private final ClientApiRedisSyncService clientApiRedisSyncService;

    public ApiDefaultConfigService(ApiDefaultConfigRepository apiDefaultConfigRepo, MicroServiceRepository microServiceRepo, ExposedApiRepository exposedApiRepo, ClientApiRepository clientApiRepo, ApiDefaultConfigResolver apiDefaultConfigResolver, ExposedApiRedisSyncService exposedApiRedisSyncService, ClientApiRedisSyncService clientApiRedisSyncService) {
        this.apiDefaultConfigRepo = apiDefaultConfigRepo;
        this.microServiceRepo = microServiceRepo;
        this.exposedApiRepo = exposedApiRepo;
        this.clientApiRepo = clientApiRepo;
        this.apiDefaultConfigResolver = apiDefaultConfigResolver;
        this.exposedApiRedisSyncService = exposedApiRedisSyncService;
        this.clientApiRedisSyncService = clientApiRedisSyncService;
    }


    public ApiDefaultConfigResponse upsert(ApiDefaultConfigUpsertRequest request) {
        validateRequest(request);

        var now = LocalDateTime.now();
        var entity = findExisting(request)
                .orElseGet(() -> {
                    var newEntity = new ApiDefaultConfigEntity();
                    newEntity.setApiType(resolveApiType(request));
                    newEntity.setScope(request.getScope());
                    newEntity.setMicroServiceId(request.getMicroServiceId());
                    newEntity.setCreatedAt(now);
                    return newEntity;
                });

        applyRequest(entity, request);
        entity.setUpdatedAt(now);
        var saved = apiDefaultConfigRepo.save(entity);

        handleApplyMode(saved, request.getApplyMode() != null ? request.getApplyMode() : DefaultApplyMode.NEW_ONLY);

        return toResponse(saved);
    }

    public List<ApiDefaultConfigResponse> getAll() {
        return apiDefaultConfigRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ApiDefaultConfigResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    public void delete(UUID id) {
        apiDefaultConfigRepo.delete(getEntity(id));
    }

    private ApiDefaultConfigEntity getEntity(UUID id) {
        return apiDefaultConfigRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.API_DEFAULT_CONFIG_NOTFOUND));
    }

    private java.util.Optional<ApiDefaultConfigEntity> findExisting(ApiDefaultConfigUpsertRequest request) {
        var apiType = resolveApiType(request);
        if (request.getScope() == DefaultConfigScope.GLOBAL) {
            return apiDefaultConfigRepo.findByApiTypeAndScopeAndMicroServiceIdIsNull(apiType, DefaultConfigScope.GLOBAL);
        }
        return apiDefaultConfigRepo.findByApiTypeAndScopeAndMicroServiceId(apiType, DefaultConfigScope.SERVICE, request.getMicroServiceId());
    }

    private void validateRequest(ApiDefaultConfigUpsertRequest request) {
        var apiType = resolveApiType(request);
        if (request.getScope() == null) {
            throw new AppException(ErrorCode.API_DEFAULT_CONFIG_INVALID);
        }
        if (request.getScope() == DefaultConfigScope.GLOBAL && request.getMicroServiceId() != null) {
            throw new AppException(ErrorCode.API_DEFAULT_CONFIG_INVALID);
        }
        if (request.getScope() == DefaultConfigScope.SERVICE) {
            if (request.getMicroServiceId() == null) {
                throw new AppException(ErrorCode.API_DEFAULT_CONFIG_INVALID);
            }
            if (!microServiceRepo.existsById(request.getMicroServiceId())) {
                throw new AppException(ErrorCode.MICROSERVICE_NOTFOUND);
            }
        }
        validatePositive(request.getMaxRequests());
        validatePositive(request.getThrottleWindowSec());
        validatePositive(request.getMaxRequestKb());
        validatePositive(request.getMaxResponseKb());
        validatePositive(request.getLatencyThresholdMs());
        validatePositive(request.getTimeoutMs());
        validatePositive(request.getLogRetentionDays());
        if (apiType == ApiConfigType.CLIENT) {
            validatePositive(request.getMaxRetries());
            validatePositive(request.getRetryDelayMs());
        }
    }

    private void validatePositive(Integer value) {
        if (value != null && value <= 0) {
            throw new AppException(ErrorCode.API_DEFAULT_CONFIG_INVALID);
        }
    }

    private void applyRequest(ApiDefaultConfigEntity entity, ApiDefaultConfigUpsertRequest request) {
        entity.setApiType(resolveApiType(request));
        entity.setMaxRequests(request.getMaxRequests());
        entity.setThrottleWindowSec(request.getThrottleWindowSec());
        entity.setMaxRequestKb(request.getMaxRequestKb());
        entity.setMaxResponseKb(request.getMaxResponseKb());
        entity.setLatencyThresholdMs(request.getLatencyThresholdMs());
        entity.setTimeoutMs(request.getTimeoutMs());
        entity.setLogRetentionDays(request.getLogRetentionDays());
        entity.setMaxRetries(request.getMaxRetries());
        entity.setRetryDelayMs(request.getRetryDelayMs());
        entity.setFailureAction(request.getFailureAction());
        entity.setNotificationRuleId(request.getNotificationRuleId());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
    }

    private void handleApplyMode(ApiDefaultConfigEntity config, DefaultApplyMode applyMode) {
        if (applyMode == DefaultApplyMode.NEW_ONLY) {
            return;
        }

        if (config.getApiType() == ApiConfigType.CLIENT) {
            handleClientApplyMode(config, applyMode);
            return;
        }

        handleExposedApplyMode(config, applyMode);
    }

    private void handleExposedApplyMode(ApiDefaultConfigEntity config, DefaultApplyMode applyMode) {
        var apis = config.getScope() == DefaultConfigScope.GLOBAL
                ? exposedApiRepo.findAll()
                : exposedApiRepo.findByMicroServiceId(config.getMicroServiceId());

        if (applyMode == DefaultApplyMode.APPLY_TO_EXISTING) {
            apis = apis.stream()
                    .filter(api -> Boolean.TRUE.equals(api.getUseDefaultConfig()))
                    .toList();
        }

        var now = LocalDateTime.now();
        for (var api : apis) {
            apiDefaultConfigResolver.applyTo(api);
            api.setUseDefaultConfig(true);
            api.setUpdatedAt(now);
        }
        exposedApiRepo.saveAll(apis);
        exposedApiRedisSyncService.syncAll(apis);
    }

    private void handleClientApplyMode(ApiDefaultConfigEntity config, DefaultApplyMode applyMode) {
        var apis = config.getScope() == DefaultConfigScope.GLOBAL
                ? clientApiRepo.findAll()
                : clientApiRepo.findByMicroServiceId(config.getMicroServiceId());

        if (applyMode == DefaultApplyMode.APPLY_TO_EXISTING) {
            apis = apis.stream()
                    .filter(api -> Boolean.TRUE.equals(api.getUseDefaultConfig()))
                    .toList();
        }

        var now = LocalDateTime.now();
        for (var api : apis) {
            apiDefaultConfigResolver.applyTo(api);
            api.setUseDefaultConfig(true);
            api.setUpdatedAt(now);
        }
        clientApiRepo.saveAll(apis);
        clientApiRedisSyncService.syncAll(apis);
    }

    private ApiDefaultConfigResponse toResponse(ApiDefaultConfigEntity entity) {
        return ApiDefaultConfigResponse.builder()
                .id(entity.getId())
                .apiType(entity.getApiType())
                .scope(entity.getScope())
                .microServiceId(entity.getMicroServiceId())
                .microServiceName(resolveMicroServiceName(entity.getMicroServiceId()))
                .maxRequests(entity.getMaxRequests())
                .throttleWindowSec(entity.getThrottleWindowSec())
                .maxRequestKb(entity.getMaxRequestKb())
                .maxResponseKb(entity.getMaxResponseKb())
                .latencyThresholdMs(entity.getLatencyThresholdMs())
                .timeoutMs(entity.getTimeoutMs())
                .logRetentionDays(entity.getLogRetentionDays())
                .maxRetries(entity.getMaxRetries())
                .retryDelayMs(entity.getRetryDelayMs())
                .failureAction(entity.getFailureAction())
                .notificationRuleId(entity.getNotificationRuleId())
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String resolveMicroServiceName(UUID microServiceId) {
        if (microServiceId == null) {
            return null;
        }
        return microServiceRepo.findById(microServiceId)
                .map(MicroServiceEntity::getName)
                .orElse(null);
    }

    private ApiConfigType resolveApiType(ApiDefaultConfigUpsertRequest request) {
        return request.getApiType() != null ? request.getApiType() : ApiConfigType.EXPOSED;
    }
}
