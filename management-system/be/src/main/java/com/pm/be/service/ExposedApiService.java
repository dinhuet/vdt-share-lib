package com.pm.be.service;

import com.pm.be.dto.request.ExposedApiLimitUpdateRequest;
import com.pm.be.dto.request.ExposedApiNotificationRuleUpdateRequest;
import com.pm.be.dto.response.ExposedApiResponse;
import com.pm.be.entity.ExposedApiEntity;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.enums.SyncStatus;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.ExposedApiRepository;
import com.pm.be.repository.MicroServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExposedApiService {

    private final ExposedApiRepository exposedApiRepo;
    private final MicroServiceRepository microServiceRepo;
    private final ApiDefaultConfigResolver apiDefaultConfigResolver;
    private final ExposedApiRedisSyncService exposedApiRedisSyncService;

    public List<ExposedApiResponse> getAll(UUID microServiceId, SyncStatus syncStatus) {
        List<ExposedApiEntity> apis;
        if (microServiceId != null) {
            if (!microServiceRepo.existsById(microServiceId)) {
                throw new AppException(ErrorCode.MICROSERVICE_NOTFOUND);
            }
            apis = exposedApiRepo.findByMicroServiceId(microServiceId);
        } else if (syncStatus != null) {
            apis = exposedApiRepo.findBySyncStatus(syncStatus);
        } else {
            apis = exposedApiRepo.findAll();
        }
        return apis.stream().map(this::toResponse).toList();
    }

    public ExposedApiResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    public ExposedApiResponse updateLimits(UUID id, ExposedApiLimitUpdateRequest request) {
        validateLimitRequest(request);
        var entity = getEntity(id);
        entity.setMaxRequests(request.getMaxRequests());
        entity.setThrottleWindowSec(request.getThrottleWindowSec());
        entity.setMaxRequestKb(request.getMaxRequestKb());
        entity.setMaxResponseKb(request.getMaxResponseKb());
        entity.setLatencyThresholdMs(request.getLatencyThresholdMs());
        entity.setTimeoutMs(request.getTimeoutMs());
        entity.setLogRetentionDays(request.getLogRetentionDays());
        entity.setUseDefaultConfig(false);
        entity.setUpdatedAt(LocalDateTime.now());
        return saveAndSync(entity);
    }

    public ExposedApiResponse useDefaultConfig(UUID id) {
        var entity = getEntity(id);
        apiDefaultConfigResolver.applyTo(entity);
        entity.setUseDefaultConfig(true);
        entity.setUpdatedAt(LocalDateTime.now());
        return saveAndSync(entity);
    }

    public ExposedApiResponse enable(UUID id) {
        return updateEnabled(id, true);
    }

    public ExposedApiResponse disable(UUID id) {
        return updateEnabled(id, false);
    }

    public ExposedApiResponse updateNotificationRule(UUID id, ExposedApiNotificationRuleUpdateRequest request) {
        var entity = getEntity(id);
        entity.setNotificationRuleId(request.getNotificationRuleId());
        entity.setUseDefaultConfig(false);
        entity.setUpdatedAt(LocalDateTime.now());
        return saveAndSync(entity);
    }

    public void delete(UUID id) {
        var entity = getEntity(id);
        if (entity.getSyncStatus() != SyncStatus.STALE) {
            throw new AppException(ErrorCode.EXPOSED_API_DELETE_NOT_ALLOWED);
        }
        exposedApiRedisSyncService.deleteApi(entity);
        exposedApiRepo.delete(entity);
    }

    private ExposedApiResponse updateEnabled(UUID id, boolean enabled) {
        var entity = getEntity(id);
        entity.setEnabled(enabled);
        entity.setUpdatedAt(LocalDateTime.now());
        return saveAndSync(entity);
    }

    private ExposedApiResponse saveAndSync(ExposedApiEntity entity) {
        var saved = exposedApiRepo.save(entity);
        exposedApiRedisSyncService.syncApi(saved);
        return toResponse(saved);
    }

    private ExposedApiEntity getEntity(UUID id) {
        return exposedApiRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EXPOSED_API_NOTFOUND));
    }

    private void validateLimitRequest(ExposedApiLimitUpdateRequest request) {
        validatePositive(request.getMaxRequests());
        validatePositive(request.getThrottleWindowSec());
        validatePositive(request.getMaxRequestKb());
        validatePositive(request.getMaxResponseKb());
        validatePositive(request.getLatencyThresholdMs());
        validatePositive(request.getTimeoutMs());
        validatePositive(request.getLogRetentionDays());
    }

    private void validatePositive(Integer value) {
        if (value != null && value <= 0) {
            throw new AppException(ErrorCode.EXPOSED_API_INVALID_SETTING);
        }
    }

    private ExposedApiResponse toResponse(ExposedApiEntity entity) {
        return ExposedApiResponse.builder()
                .id(entity.getId())
                .microServiceId(entity.getMicroServiceId())
                .microServiceName(resolveMicroServiceName(entity.getMicroServiceId()))
                .endpointId(entity.getEndpointId())
                .endpointKey(entity.getEndpointKey())
                .name(entity.getName())
                .path(entity.getPath())
                .method(entity.getMethod())
                .topic(entity.getTopic())
                .protocol(entity.getProtocol())
                .maxRequests(entity.getMaxRequests())
                .throttleWindowSec(entity.getThrottleWindowSec())
                .maxRequestKb(entity.getMaxRequestKb())
                .maxResponseKb(entity.getMaxResponseKb())
                .latencyThresholdMs(entity.getLatencyThresholdMs())
                .timeoutMs(entity.getTimeoutMs())
                .logRetentionDays(entity.getLogRetentionDays())
                .useDefaultConfig(entity.getUseDefaultConfig())
                .notificationRuleId(entity.getNotificationRuleId())
                .enabled(entity.getEnabled())
                .registrationSource(entity.getRegistrationSource())
                .syncStatus(entity.getSyncStatus())
                .lastSyncedAt(entity.getLastSyncedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String resolveMicroServiceName(UUID microServiceId) {
        return microServiceRepo.findById(microServiceId)
                .map(MicroServiceEntity::getName)
                .orElse(null);
    }
}
