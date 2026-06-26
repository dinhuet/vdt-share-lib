package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyNotificationProperties;
import com.pm.be.dto.request.anomaly.SecurityAlertTemporaryBlacklistRequest;
import com.pm.be.entity.accesspolicy.AccessPolicyEntity;
import com.pm.be.entity.anomaly.SecurityAlertActionEntity;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.entity.exposedapi.ExposedApiEntity;
import com.pm.be.enums.AccessPolicyMatchType;
import com.pm.be.enums.AccessPolicyType;
import com.pm.be.enums.SecurityAlertActionTargetType;
import com.pm.be.enums.SecurityAlertActionType;
import com.pm.be.enums.SecurityAlertStatus;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.accesspolicy.AccessPolicyRepository;
import com.pm.be.repository.anomaly.SecurityAlertActionRepository;
import com.pm.be.repository.anomaly.SecurityAlertRepository;
import com.pm.be.repository.exposedapi.ExposedApiRepository;
import com.pm.be.service.accesspolicy.AccessPolicyRedisSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAlertBlacklistService {
    private static final int MIN_DURATION_MINUTES = 5;
    private static final int MAX_DURATION_MINUTES = 1440;
    private static final Pattern IP_CHAR_PATTERN = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final SecurityAlertRepository alertRepository;
    private final SecurityAlertActionRepository actionRepository;
    private final AccessPolicyRepository accessPolicyRepository;
    private final ExposedApiRepository exposedApiRepository;
    private final AccessPolicyRedisSyncService redisSyncService;
    private final AnomalyNotificationProperties properties;

    @Transactional
    public Optional<AccessPolicyEntity> autoBlacklistCritical(SecurityAlertEntity alert) {
        if (alert == null || alert.getId() == null || !isAutoBlacklistEnabled()) {
            return Optional.empty();
        }
        try {
            UUID exposedApiId = resolveExposedApiId(alert.getEndpointId()).orElse(null);
            if (exposedApiId == null) {
                log.warn("Skip CRITICAL auto blacklist because alert endpoint is not a known exposed API: alertId={} endpointId={}", alert.getId(), alert.getEndpointId());
                return Optional.empty();
            }
            Target target = autoTarget(alert).orElse(null);
            if (target == null) {
                log.warn("Skip CRITICAL auto blacklist because no valid target was found: alertId={}", alert.getId());
                return Optional.empty();
            }
            int duration = normalizeDuration(properties.getCriticalAutoBlacklist().getDurationMinutes());
            return Optional.of(upsertPolicyAndAction(alert, exposedApiId, target, duration,
                    "Auto temporary blacklist for CRITICAL anomaly", "SYSTEM", false));
        } catch (RuntimeException e) {
            log.warn("Failed to auto blacklist CRITICAL alert; notification flow will continue: alertId={}", alert.getId(), e);
            return Optional.empty();
        }
    }

    @Transactional
    public SecurityAlertEntity temporaryBlacklist(UUID alertId, SecurityAlertTemporaryBlacklistRequest request) {
        SecurityAlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AppException(ErrorCode.SECURITY_ALERT_NOTFOUND));
        UUID exposedApiId = resolveExposedApiId(alert.getEndpointId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_POLICY_INVALID));
        Target target = validateManualTarget(request);
        int duration = normalizeDuration(request.getDurationMinutes());
        String user = resolveCurrentUser();
        upsertPolicyAndAction(alert, exposedApiId, target, duration, trimToNull(request.getReason()), user, true);
        alert.setStatus(SecurityAlertStatus.ACKED);
        alert.setAcknowledgedBy(user);
        alert.setAcknowledgedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }

    private AccessPolicyEntity upsertPolicyAndAction(SecurityAlertEntity alert, UUID exposedApiId, Target target,
                                                     int durationMinutes, String reason, String user, boolean ackAlert) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(durationMinutes);
        AccessPolicyEntity policy = accessPolicyRepository
                .findByExposedApiIdAndMatchTypeAndMatchValue(exposedApiId, target.matchType(), target.value())
                .orElseGet(() -> AccessPolicyEntity.builder()
                        .exposedApiId(exposedApiId)
                        .matchType(target.matchType())
                        .matchValue(target.value())
                        .createdBy(user)
                        .createdAt(now)
                        .build());
        policy.setType(AccessPolicyType.BLACK);
        policy.setTemporary(true);
        policy.setExpiresAt(expiresAt);
        AccessPolicyEntity saved = accessPolicyRepository.save(policy);
        actionRepository.save(SecurityAlertActionEntity.builder()
                .alertId(alert.getId())
                .actionType(SecurityAlertActionType.TEMP_BLACKLIST)
                .targetType(target.actionTargetType())
                .targetValue(target.value())
                .durationMinutes(durationMinutes)
                .reason(reason)
                .createdBy(user)
                .build());
        redisSyncService.syncByExposedApiId(exposedApiId);
        if (ackAlert) {
            log.info("Temporary blacklist applied from alert: alertId={} targetType={} targetValue={}", alert.getId(), target.actionTargetType(), target.value());
        }
        return saved;
    }

    private Optional<UUID> resolveExposedApiId(String endpointId) {
        if (!StringUtils.hasText(endpointId)) {
            return Optional.empty();
        }
        try {
            UUID id = UUID.fromString(endpointId.trim());
            if (exposedApiRepository.existsById(id)) {
                return Optional.of(id);
            }
            return exposedApiRepository.findByEndpointId(id)
                    .map(ExposedApiEntity::getId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<Target> autoTarget(SecurityAlertEntity alert) {
        boolean preferClient = properties.getCriticalAutoBlacklist() == null
                || !Boolean.FALSE.equals(properties.getCriticalAutoBlacklist().getPreferClient());
        if (preferClient && StringUtils.hasText(alert.getClientId())) {
            return Optional.of(new Target(SecurityAlertActionTargetType.CLIENT, AccessPolicyMatchType.CLIENT_ID, alert.getClientId().trim()));
        }
        if (StringUtils.hasText(alert.getSourceIp()) && isValidIp(alert.getSourceIp().trim())) {
            return Optional.of(new Target(SecurityAlertActionTargetType.IP, AccessPolicyMatchType.IP, alert.getSourceIp().trim()));
        }
        if (!preferClient && StringUtils.hasText(alert.getClientId())) {
            return Optional.of(new Target(SecurityAlertActionTargetType.CLIENT, AccessPolicyMatchType.CLIENT_ID, alert.getClientId().trim()));
        }
        return Optional.empty();
    }

    private Target validateManualTarget(SecurityAlertTemporaryBlacklistRequest request) {
        if (request == null || request.getTargetType() == null || !StringUtils.hasText(request.getTargetValue())) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
        String value = request.getTargetValue().trim();
        return switch (request.getTargetType()) {
            case CLIENT -> new Target(SecurityAlertActionTargetType.CLIENT, AccessPolicyMatchType.CLIENT_ID, value);
            case IP -> {
                if (!isValidIp(value)) throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
                yield new Target(SecurityAlertActionTargetType.IP, AccessPolicyMatchType.IP, value);
            }
            case CIDR -> {
                validateCidr(value);
                yield new Target(SecurityAlertActionTargetType.CIDR, AccessPolicyMatchType.CIDR, value);
            }
            default -> throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        };
    }

    private int normalizeDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
        return durationMinutes;
    }

    private boolean isAutoBlacklistEnabled() {
        return properties.getCriticalAutoBlacklist() != null
                && Boolean.TRUE.equals(properties.getCriticalAutoBlacklist().getEnabled());
    }

    private boolean isValidIp(String value) {
        if (!StringUtils.hasText(value) || !IP_CHAR_PATTERN.matcher(value).matches() || (!value.contains(".") && !value.contains(":"))) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateCidr(String value) {
        String[] parts = value.split("/", -1);
        if (parts.length != 2 || !isValidIp(parts[0])) {
            throw new AppException(ErrorCode.ACCESS_POLICY_INVALID);
        }
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

    private String resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record Target(SecurityAlertActionTargetType actionTargetType, AccessPolicyMatchType matchType, String value) {
    }
}
