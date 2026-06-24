package com.pm.be.service.clientpermission;

import com.pm.be.dto.request.clientpermission.ClientPermissionGrantRequest;
import com.pm.be.dto.response.clientpermission.ClientPermissionResponse;
import com.pm.be.entity.client.ClientEntity;
import com.pm.be.entity.clientpermission.ClientExposedApiPermissionEntity;
import com.pm.be.entity.exposedapi.ExposedApiEntity;
import com.pm.be.entity.microservice.MicroServiceEntity;
import com.pm.be.enums.ClientStatus;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.clientpermission.ClientExposedApiPermissionRepository;
import com.pm.be.repository.client.ClientRepository;
import com.pm.be.repository.exposedapi.ExposedApiRepository;
import com.pm.be.repository.microservice.MicroServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientPermissionService {

    private final ClientExposedApiPermissionRepository permissionRepo;
    private final ClientRepository clientRepo;
    private final ExposedApiRepository exposedApiRepo;
    private final MicroServiceRepository microServiceRepo;
    private final ClientPermissionRedisSyncService clientPermissionRedisSyncService;

    public List<ClientPermissionResponse> getByClient(UUID clientId) {
        var client = getClient(clientId);
        return permissionRepo.findByClientId(clientId).stream()
                .map(permission -> toResponse(permission, client, resolveExposedApi(permission.getExposedApiId())))
                .toList();
    }

    public ClientPermissionResponse grant(UUID clientId, ClientPermissionGrantRequest request) {
        var client = getClient(clientId);
        ensureClientActive(client);
        if (request == null || request.getExposedApiId() == null) {
            throw new AppException(ErrorCode.CLIENT_PERMISSION_INVALID);
        }
        var exposedApi = getExposedApi(request.getExposedApiId());
        if (permissionRepo.existsByClientIdAndExposedApiId(clientId, request.getExposedApiId())) {
            throw new AppException(ErrorCode.CLIENT_PERMISSION_EXISTED);
        }

        var now = LocalDateTime.now();
        var entity = ClientExposedApiPermissionEntity.builder()
                .clientId(clientId)
                .exposedApiId(request.getExposedApiId())
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        var saved = permissionRepo.save(entity);
        clientPermissionRedisSyncService.syncPermission(saved);
        return toResponse(saved, client, exposedApi);
    }

    public ClientPermissionResponse enable(UUID clientId, UUID permissionId) {
        var client = getClient(clientId);
        ensureClientActive(client);
        var entity = getPermission(clientId, permissionId);
        entity.setEnabled(true);
        entity.setUpdatedAt(LocalDateTime.now());
        var saved = permissionRepo.save(entity);
        clientPermissionRedisSyncService.syncPermission(saved);
        return toResponse(saved, client, resolveExposedApi(saved.getExposedApiId()));
    }

    public ClientPermissionResponse disable(UUID clientId, UUID permissionId) {
        var client = getClient(clientId);
        var entity = getPermission(clientId, permissionId);
        entity.setEnabled(false);
        entity.setUpdatedAt(LocalDateTime.now());
        var saved = permissionRepo.save(entity);
        clientPermissionRedisSyncService.syncPermission(saved);
        return toResponse(saved, client, resolveExposedApi(saved.getExposedApiId()));
    }

    public void delete(UUID clientId, UUID permissionId) {
        getClient(clientId);
        var entity = getPermission(clientId, permissionId);
        permissionRepo.delete(entity);
        clientPermissionRedisSyncService.deletePermission(entity);
    }

    private ClientEntity getClient(UUID clientId) {
        return clientRepo.findById(clientId)
                .orElseThrow(() -> new AppException(ErrorCode.CLIENT_NOTFOUND));
    }

    private ExposedApiEntity getExposedApi(UUID exposedApiId) {
        return exposedApiRepo.findById(exposedApiId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPOSED_API_NOTFOUND));
    }

    private ExposedApiEntity resolveExposedApi(UUID exposedApiId) {
        return exposedApiRepo.findById(exposedApiId).orElse(null);
    }

    private ClientExposedApiPermissionEntity getPermission(UUID clientId, UUID permissionId) {
        var entity = permissionRepo.findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.CLIENT_PERMISSION_NOTFOUND));
        if (!entity.getClientId().equals(clientId)) {
            throw new AppException(ErrorCode.CLIENT_PERMISSION_NOTFOUND);
        }
        return entity;
    }

    private void ensureClientActive(ClientEntity client) {
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new AppException(ErrorCode.CLIENT_STATUS_INVALID);
        }
    }

    private ClientPermissionResponse toResponse(
            ClientExposedApiPermissionEntity permission,
            ClientEntity client,
            ExposedApiEntity exposedApi) {
        var microService = exposedApi != null ? resolveMicroService(exposedApi.getMicroServiceId()) : null;
        return ClientPermissionResponse.builder()
                .id(permission.getId())
                .clientId(permission.getClientId())
                .clientCode(client.getClientCode())
                .clientName(client.getName())
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

    private MicroServiceEntity resolveMicroService(UUID microServiceId) {
        return microServiceRepo.findById(microServiceId).orElse(null);
    }
}
