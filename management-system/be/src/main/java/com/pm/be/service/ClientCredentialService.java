package com.pm.be.service;

import com.pm.be.dto.request.ClientCredentialCreateRequest;
import com.pm.be.dto.request.ClientCredentialRevokeRequest;
import com.pm.be.dto.response.ClientCredentialCreatedResponse;
import com.pm.be.dto.response.ClientCredentialResponse;
import com.pm.be.entity.ClientCredentialEntity;
import com.pm.be.entity.ClientEntity;
import com.pm.be.entity.MicroServiceEntity;
import com.pm.be.enums.ClientCredentialStatus;
import com.pm.be.enums.ClientStatus;
import com.pm.be.enums.CredentialExpiryState;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.ClientCredentialRepository;
import com.pm.be.repository.ClientRepository;
import com.pm.be.repository.MicroServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientCredentialService {
    private static final String DEFAULT_ALGORITHM = "HMAC-SHA256";
    private static final int EXPIRING_SOON_DAYS = 7;
    private static final int KEY_ID_RANDOM_LENGTH = 8;
    private static final int KEY_ID_GENERATE_MAX_ATTEMPTS = 10;

    private final ClientCredentialRepository credentialRepo;
    private final ClientRepository clientRepo;
    private final MicroServiceRepository microServiceRepo;
    private final CredentialSecretService credentialSecretService;

    public List<ClientCredentialResponse> getAll(UUID clientId, UUID microServiceId) {
        var client = getClient(clientId);
        List<ClientCredentialEntity> credentials = microServiceId == null
                ? credentialRepo.findByClientId(clientId)
                : credentialRepo.findByClientIdAndMicroServiceId(clientId, microServiceId);
        return credentials.stream()
                .map(credential -> toResponse(credential, client, resolveMicroService(credential.getMicroServiceId())))
                .toList();
    }

    public ClientCredentialCreatedResponse create(UUID clientId, ClientCredentialCreateRequest request) {
        validateCreateRequest(request);
        var client = getClient(clientId);
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new AppException(ErrorCode.CLIENT_STATUS_INVALID);
        }
        var microService = microServiceRepo.findById(request.getMicroServiceId())
                .orElseThrow(() -> new AppException(ErrorCode.MICROSERVICE_NOTFOUND));

        String keyId = resolveKeyId(request.getKeyId());
        if (credentialRepo.existsByKeyId(keyId)) {
            throw new AppException(ErrorCode.CLIENT_CREDENTIAL_EXISTED);
        }

        String apiKey = credentialSecretService.generateApiKey();
        String signingSecret = credentialSecretService.generateSigningSecret();
        var now = LocalDateTime.now();
        var entity = ClientCredentialEntity.builder()
                .clientId(clientId)
                .microServiceId(request.getMicroServiceId())
                .keyId(keyId)
                .apiKeyHash(credentialSecretService.hashApiKey(apiKey))
                .signingSecretEncrypted(credentialSecretService.encryptSigningSecret(signingSecret))
                .algorithm(DEFAULT_ALGORITHM)
                .status(ClientCredentialStatus.ACTIVE)
                .expiresAt(request.getExpiresAt())
                .createdAt(now)
                .updatedAt(now)
                .build();

        var saved = credentialRepo.save(entity);
        return new ClientCredentialCreatedResponse(toResponse(saved, client, microService), apiKey, signingSecret);
    }

    public ClientCredentialResponse revoke(UUID clientId, UUID credentialId, ClientCredentialRevokeRequest request) {
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new AppException(ErrorCode.CLIENT_CREDENTIAL_INVALID);
        }
        var client = getClient(clientId);
        var entity = getCredential(clientId, credentialId);
        if (entity.getStatus() != ClientCredentialStatus.ACTIVE) {
            throw new AppException(ErrorCode.CLIENT_CREDENTIAL_REVOKE_NOT_ALLOWED);
        }

        var now = LocalDateTime.now();
        entity.setStatus(ClientCredentialStatus.REVOKED);
        entity.setRevokedAt(now);
        entity.setRevokedBy(resolveCurrentUser());
        entity.setRevokeReason(request.getReason().trim());
        entity.setUpdatedAt(now);

        var saved = credentialRepo.save(entity);
        return toResponse(saved, client, resolveMicroService(saved.getMicroServiceId()));
    }

    private void validateCreateRequest(ClientCredentialCreateRequest request) {
        if (request == null || request.getMicroServiceId() == null) {
            throw new AppException(ErrorCode.CLIENT_CREDENTIAL_INVALID);
        }
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.CLIENT_CREDENTIAL_INVALID);
        }
    }

    private ClientEntity getClient(UUID clientId) {
        return clientRepo.findById(clientId)
                .orElseThrow(() -> new AppException(ErrorCode.CLIENT_NOTFOUND));
    }

    private ClientCredentialEntity getCredential(UUID clientId, UUID credentialId) {
        var entity = credentialRepo.findById(credentialId)
                .orElseThrow(() -> new AppException(ErrorCode.CLIENT_CREDENTIAL_NOTFOUND));
        if (!entity.getClientId().equals(clientId)) {
            throw new AppException(ErrorCode.CLIENT_CREDENTIAL_NOTFOUND);
        }
        return entity;
    }

    private MicroServiceEntity resolveMicroService(UUID microServiceId) {
        return microServiceRepo.findById(microServiceId).orElse(null);
    }

    private String resolveKeyId(String keyId) {
        if (StringUtils.hasText(keyId)) {
            return keyId.trim();
        }
        for (int attempt = 0; attempt < KEY_ID_GENERATE_MAX_ATTEMPTS; attempt++) {
            String generatedKeyId = "key-" + UUID.randomUUID().toString().substring(0, KEY_ID_RANDOM_LENGTH);
            if (!credentialRepo.existsByKeyId(generatedKeyId)) {
                return generatedKeyId;
            }
        }
        throw new AppException(ErrorCode.CLIENT_CREDENTIAL_EXISTED);
    }

    private CredentialExpiryState resolveExpiryState(ClientCredentialEntity entity) {
        if (entity.getStatus() == ClientCredentialStatus.REVOKED) {
            return CredentialExpiryState.REVOKED;
        }
        if (entity.getExpiresAt() == null) {
            return CredentialExpiryState.NO_EXPIRY;
        }
        var now = LocalDateTime.now();
        if (entity.getExpiresAt().isBefore(now)) {
            return CredentialExpiryState.EXPIRED;
        }
        if (!entity.getExpiresAt().isAfter(now.plusDays(EXPIRING_SOON_DAYS))) {
            return CredentialExpiryState.EXPIRING_SOON;
        }
        return CredentialExpiryState.VALID;
    }

    private Long resolveDaysUntilExpiry(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }

    private String resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private ClientCredentialResponse toResponse(ClientCredentialEntity entity, ClientEntity client, MicroServiceEntity microService) {
        return ClientCredentialResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .clientCode(client.getClientCode())
                .microServiceId(entity.getMicroServiceId())
                .microServiceName(microService != null ? microService.getName() : null)
                .keyId(entity.getKeyId())
                .algorithm(entity.getAlgorithm())
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .expiryState(resolveExpiryState(entity))
                .daysUntilExpiry(resolveDaysUntilExpiry(entity.getExpiresAt()))
                .revokedAt(entity.getRevokedAt())
                .revokedBy(entity.getRevokedBy())
                .revokeReason(entity.getRevokeReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
