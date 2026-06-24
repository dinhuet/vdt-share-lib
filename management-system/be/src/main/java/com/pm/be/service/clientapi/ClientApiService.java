package com.pm.be.service.clientapi;

import com.pm.be.dto.request.clientapi.ClientApiUpdateRequest;
import com.pm.be.dto.response.clientapi.ClientApiResponse;
import com.pm.be.entity.clientapi.ClientApiEntity;
import com.pm.be.entity.microservice.MicroServiceEntity;
import com.pm.be.enums.SyncStatus;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.clientapi.ClientApiRepository;
import com.pm.be.repository.microservice.MicroServiceRepository;
import com.pm.be.service.apidefaultconfig.ApiDefaultConfigResolver;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientApiService {

    private final ClientApiRepository clientApiRepo;
    private final MicroServiceRepository microServiceRepo;
    private final ClientApiRedisSyncService clientApiRedisSyncService;
    private final ApiDefaultConfigResolver apiDefaultConfigResolver;

    public List<ClientApiResponse> getAll(UUID microServiceId, UUID clientId, Boolean enabled, SyncStatus syncStatus) {
        return clientApiRepo.findAll(buildSpec(microServiceId, clientId, enabled, syncStatus)).stream()
                .map(this::toResponse)
                .toList();
    }

    public ClientApiResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    public ClientApiResponse update(UUID id, ClientApiUpdateRequest request) {
        validateRequest(request.getMicroServiceId(), request.getName(), request.getProtocol());
        validateSettings(request.getLatencyThresholdMs(), request.getTimeoutMs(), request.getMaxRetries(),
                request.getRetryDelayMs(), request.getLogRetentionDays());

        if (!microServiceRepo.existsById(request.getMicroServiceId())) {
            throw new AppException(ErrorCode.MICROSERVICE_NOTFOUND);
        }

        var entity = getEntity(id);
        clientApiRepo.findByMicroServiceIdAndName(request.getMicroServiceId(), request.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.CLIENT_API_EXISTED);
                });

        applyUpdateRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = clientApiRepo.save(entity);
        clientApiRedisSyncService.syncApi(saved);
        return toResponse(saved);
    }

    public ClientApiResponse enable(UUID id) {
        return updateEnabled(id, true);
    }

    public ClientApiResponse disable(UUID id) {
        return updateEnabled(id, false);
    }

    public void delete(UUID id) {
        var entity = getEntity(id);
        if (entity.getSyncStatus() != SyncStatus.STALE) {
            throw new AppException(ErrorCode.CLIENT_API_DELETE_NOT_ALLOWED);
        }
        clientApiRedisSyncService.deleteApi(entity);
        clientApiRepo.delete(entity);
    }

    public ClientApiResponse useDefaultConfig(UUID id) {
        var entity = getEntity(id);
        apiDefaultConfigResolver.applyTo(entity);
        entity.setUseDefaultConfig(true);
        entity.setUpdatedAt(LocalDateTime.now());
        var saved = clientApiRepo.save(entity);
        clientApiRedisSyncService.syncApi(saved);
        return toResponse(saved);
    }

    private ClientApiResponse updateEnabled(UUID id, boolean enabled) {
        var entity = getEntity(id);
        entity.setEnabled(enabled);
        entity.setUpdatedAt(LocalDateTime.now());
        var saved = clientApiRepo.save(entity);
        clientApiRedisSyncService.syncApi(saved);
        return toResponse(saved);
    }

    private ClientApiEntity getEntity(UUID id) {
        return clientApiRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLIENT_API_NOTFOUND));
    }

    private Specification<ClientApiEntity> buildSpec(UUID microServiceId, UUID clientId, Boolean enabled, SyncStatus syncStatus) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (microServiceId != null) {
                predicates.add(criteriaBuilder.equal(root.get("microServiceId"), microServiceId));
            }
            if (clientId != null) {
                predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
            }
            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }
            if (syncStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("syncStatus"), syncStatus));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validateRequest(UUID microServiceId, String name, String protocol) {
        if (microServiceId == null || !StringUtils.hasText(name) || !StringUtils.hasText(protocol)) {
            throw new AppException(ErrorCode.CLIENT_API_INVALID);
        }
    }

    private void validateSettings(Integer latencyThresholdMs, Integer timeoutMs, Integer maxRetries,
                                  Integer retryDelayMs, Integer logRetentionDays) {
        validatePositive(latencyThresholdMs);
        validatePositive(timeoutMs);
        validatePositive(maxRetries);
        validatePositive(retryDelayMs);
        validatePositive(logRetentionDays);
    }

    private void validatePositive(Integer value) {
        if (value != null && value <= 0) {
            throw new AppException(ErrorCode.CLIENT_API_INVALID);
        }
    }

    private void applyUpdateRequest(ClientApiEntity entity, ClientApiUpdateRequest request) {
        entity.setMicroServiceId(request.getMicroServiceId());
        entity.setClientId(request.getClientId());
        entity.setName(request.getName());
        entity.setDestinationUrl(request.getDestinationUrl());
        entity.setMethod(request.getMethod());
        entity.setProtocol(request.getProtocol());
        entity.setLatencyThresholdMs(request.getLatencyThresholdMs());
        entity.setTimeoutMs(request.getTimeoutMs());
        entity.setMaxRetries(request.getMaxRetries());
        entity.setRetryDelayMs(request.getRetryDelayMs());
        entity.setFailureAction(request.getFailureAction());
        entity.setLogRetentionDays(request.getLogRetentionDays());
        entity.setUseDefaultConfig(false);
        entity.setNotificationRuleId(request.getNotificationRuleId());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : entity.getEnabled());
    }

    private ClientApiResponse toResponse(ClientApiEntity entity) {
        return ClientApiResponse.builder()
                .id(entity.getId())
                .microServiceId(entity.getMicroServiceId())
                .microServiceName(resolveMicroServiceName(entity.getMicroServiceId()))
                .endpointId(entity.getEndpointId())
                .endpointKey(entity.getEndpointKey())
                .clientId(entity.getClientId())
                .name(entity.getName())
                .destinationUrl(entity.getDestinationUrl())
                .topic(entity.getTopic())
                .method(entity.getMethod())
                .protocol(entity.getProtocol())
                .latencyThresholdMs(entity.getLatencyThresholdMs())
                .timeoutMs(entity.getTimeoutMs())
                .maxRetries(entity.getMaxRetries())
                .retryDelayMs(entity.getRetryDelayMs())
                .failureAction(entity.getFailureAction())
                .logRetentionDays(entity.getLogRetentionDays())
                .useDefaultConfig(entity.getUseDefaultConfig())
                .notificationRuleId(entity.getNotificationRuleId())
                .enabled(entity.getEnabled())
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
