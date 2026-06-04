package com.pm.be.service;

import com.pm.be.dto.request.AccessPolicyUpsertRequest;
import com.pm.be.dto.response.AccessPolicyResponse;
import com.pm.be.entity.AccessPolicyEntity;
import com.pm.be.enums.AccessPolicyMatchType;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.AccessPolicyRepository;
import com.pm.be.repository.ClientRepository;
import com.pm.be.repository.ExposedApiRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class AccessPolicyService {
    private static final Pattern IP_CHAR_PATTERN = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final AccessPolicyRepository accessPolicyRepo;
    private final ExposedApiRepository exposedApiRepo;
    private final ClientRepository clientRepo;
    private final AccessPolicyRedisSyncService accessPolicyRedisSyncService;

    public List<AccessPolicyResponse> getAll(UUID exposedApiId) {
        ensureExposedApiExists(exposedApiId);
        return accessPolicyRepo.findByExposedApiId(exposedApiId).stream()
                .map(this::toResponse)
                .toList();
    }

    public AccessPolicyResponse create(UUID exposedApiId, AccessPolicyUpsertRequest request) {
        ensureExposedApiExists(exposedApiId);
        String matchValue = validateRequest(request);
        boolean temporary = Boolean.TRUE.equals(request.getTemporary());
        if (accessPolicyRepo.existsByExposedApiIdAndMatchTypeAndMatchValue(
                exposedApiId,
                request.getMatchType(),
                matchValue)) {
            throw new AppException(ErrorCode.ACCESS_POLICY_EXISTED);
        }

        var now = LocalDateTime.now();
        var entity = AccessPolicyEntity.builder()
                .exposedApiId(exposedApiId)
                .type(request.getType())
                .matchType(request.getMatchType())
                .matchValue(matchValue)
                .temporary(temporary)
                .expiresAt(temporary ? request.getExpiresAt() : null)
                .createdBy(resolveCurrentUser())
                .createdAt(now)
                .build();
        var saved = accessPolicyRepo.save(entity);
        accessPolicyRedisSyncService.syncByExposedApiId(exposedApiId);
        return toResponse(saved);
    }

    public AccessPolicyResponse update(UUID exposedApiId, UUID policyId, AccessPolicyUpsertRequest request) {
        ensureExposedApiExists(exposedApiId);
        String matchValue = validateRequest(request);
        boolean temporary = Boolean.TRUE.equals(request.getTemporary());
        var entity = getPolicy(exposedApiId, policyId);
        accessPolicyRepo.findByExposedApiIdAndMatchTypeAndMatchValue(
                        exposedApiId,
                        request.getMatchType(),
                        matchValue)
                .filter(existing -> !existing.getId().equals(policyId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.ACCESS_POLICY_EXISTED);
                });

        entity.setType(request.getType());
        entity.setMatchType(request.getMatchType());
        entity.setMatchValue(matchValue);
        entity.setTemporary(temporary);
        entity.setExpiresAt(temporary ? request.getExpiresAt() : null);
        var saved = accessPolicyRepo.save(entity);
        accessPolicyRedisSyncService.syncByExposedApiId(exposedApiId);
        return toResponse(saved);
    }

    public void delete(UUID exposedApiId, UUID policyId) {
        ensureExposedApiExists(exposedApiId);
        accessPolicyRepo.delete(getPolicy(exposedApiId, policyId));
        accessPolicyRedisSyncService.syncByExposedApiId(exposedApiId);
    }

    private AccessPolicyEntity getPolicy(UUID exposedApiId, UUID policyId) {
        var entity = accessPolicyRepo.findById(policyId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_POLICY_NOTFOUND));
        if (!entity.getExposedApiId().equals(exposedApiId)) {
            throw new AppException(ErrorCode.ACCESS_POLICY_NOTFOUND);
        }
        return entity;
    }

    private void ensureExposedApiExists(UUID exposedApiId) {
        if (exposedApiId == null || !exposedApiRepo.existsById(exposedApiId)) {
            throw new AppException(ErrorCode.EXPOSED_API_NOTFOUND);
        }
    }

    private String validateRequest(AccessPolicyUpsertRequest request) {
        if (request == null
                || request.getType() == null
                || request.getMatchType() == null
                || !StringUtils.hasText(request.getMatchValue())) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
        boolean temporary = Boolean.TRUE.equals(request.getTemporary());
        if (temporary && request.getExpiresAt() == null) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }

        String matchValue = request.getMatchValue().trim();
        validateMatchValue(request.getMatchType(), matchValue);
        return matchValue;
    }

    private void validateMatchValue(AccessPolicyMatchType matchType, String matchValue) {
        switch (matchType) {
            case CLIENT_ID -> validateClientId(matchValue);
            case IP -> validateIp(matchValue);
            case CIDR -> validateCidr(matchValue);
        }
    }

    private void validateClientId(String matchValue) {
        try {
            UUID clientId = UUID.fromString(matchValue);
            if (!clientRepo.existsById(clientId)) {
                throw new AppException(ErrorCode.CLIENT_NOTFOUND);
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
    }

    private void validateIp(String matchValue) {
        if (!isIpLiteral(matchValue)) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
        try {
            InetAddress.getByName(matchValue);
        } catch (Exception e) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
    }

    private void validateCidr(String matchValue) {
        String[] parts = matchValue.split("/", -1);
        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
        validateIp(parts[0]);
        try {
            int prefixLength = Integer.parseInt(parts[1]);
            int maxPrefixLength = parts[0].contains(":") ? 128 : 32;
            if (prefixLength < 0 || prefixLength > maxPrefixLength) {
                throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
            }
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
    }

    private boolean isIpLiteral(String value) {
        return IP_CHAR_PATTERN.matcher(value).matches() && (value.contains(".") || value.contains(":"));
    }

    private String resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private AccessPolicyResponse toResponse(AccessPolicyEntity entity) {
        return AccessPolicyResponse.builder()
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
