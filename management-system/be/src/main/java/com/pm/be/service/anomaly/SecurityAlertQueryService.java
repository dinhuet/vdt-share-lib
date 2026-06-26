package com.pm.be.service.anomaly;

import com.pm.be.dto.response.anomaly.NotificationDeliveryResponse;
import com.pm.be.dto.response.anomaly.SecurityAlertOccurrenceResponse;
import com.pm.be.dto.response.anomaly.SecurityAlertResponse;
import com.pm.be.dto.response.anomaly.SecurityAlertSummaryResponse;
import com.pm.be.entity.anomaly.NotificationDeliveryEntity;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.entity.anomaly.SecurityAlertOccurrenceEntity;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.SecurityAlertStatus;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.anomaly.SecurityAlertRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityAlertQueryService {
    private final SecurityAlertRepository alertRepository;
    private final SecurityAlertOccurrenceService occurrenceService;
    private final NotificationDeliveryService deliveryService;

    public List<SecurityAlertResponse> search(SecurityAlertStatus status, AnomalySeverity severity, String ruleCode,
                                              String ruleType, String serviceName, String endpointId, String clientId,
                                              String sourceIp, Instant from, Instant to) {
        return alertRepository.findAll(spec(status, severity, ruleCode, serviceName, endpointId, clientId, sourceIp, from, to),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(alert -> !StringUtils.hasText(ruleType) || ruleTypeMatches(alert, ruleType))
                .map(this::toResponse)
                .toList();
    }

    public SecurityAlertResponse getById(UUID id) {
        return toResponse(getAlert(id));
    }

    public List<SecurityAlertOccurrenceResponse> occurrences(UUID alertId) {
        ensureAlertExists(alertId);
        return occurrenceService.findByAlertId(alertId).stream().map(this::toResponse).toList();
    }

    public List<NotificationDeliveryResponse> notifications(UUID alertId) {
        ensureAlertExists(alertId);
        return deliveryService.findByAlertId(alertId).stream().map(this::toResponse).toList();
    }

    public SecurityAlertSummaryResponse summary() {
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        return SecurityAlertSummaryResponse.builder()
                .openCount(alertRepository.countByStatus(SecurityAlertStatus.OPEN))
                .mediumOpenCount(alertRepository.countByStatusAndSeverity(SecurityAlertStatus.OPEN, AnomalySeverity.MEDIUM))
                .highOpenCount(alertRepository.countByStatusAndSeverity(SecurityAlertStatus.OPEN, AnomalySeverity.HIGH))
                .criticalOpenCount(alertRepository.countByStatusAndSeverity(SecurityAlertStatus.OPEN, AnomalySeverity.CRITICAL))
                .recent24hCount(alertRepository.countByCreatedAtGreaterThanEqual(last24h))
                .latestAlertAt(alertRepository.findFirstByOrderByCreatedAtDesc().map(SecurityAlertEntity::getCreatedAt).orElse(null))
                .build();
    }

    public List<SecurityAlertResponse> recent(Integer limit) {
        int normalizedLimit = limit == null ? 5 : Math.max(1, Math.min(limit, 50));
        return alertRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, normalizedLimit)).stream()
                .map(this::toResponse)
                .toList();
    }

    private void ensureAlertExists(UUID id) {
        if (!alertRepository.existsById(id)) {
            throw new AppException(ErrorCode.SECURITY_ALERT_NOTFOUND);
        }
    }

    private SecurityAlertEntity getAlert(UUID id) {
        return alertRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.SECURITY_ALERT_NOTFOUND));
    }

    private Specification<SecurityAlertEntity> spec(SecurityAlertStatus status, AnomalySeverity severity, String ruleCode,
                                                    String serviceName, String endpointId, String clientId, String sourceIp,
                                                    Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (severity != null) predicates.add(cb.equal(root.get("severity"), severity));
            if (StringUtils.hasText(ruleCode)) predicates.add(cb.equal(root.get("alertType"), ruleCode.trim()));
            if (StringUtils.hasText(serviceName)) predicates.add(cb.equal(root.get("serviceName"), serviceName.trim()));
            if (StringUtils.hasText(endpointId)) predicates.add(cb.equal(root.get("endpointId"), endpointId.trim()));
            if (StringUtils.hasText(clientId)) predicates.add(cb.equal(root.get("clientId"), clientId.trim()));
            if (StringUtils.hasText(sourceIp)) predicates.add(cb.equal(root.get("sourceIp"), sourceIp.trim()));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), toLocalDateTime(from)));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toLocalDateTime(to)));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private boolean ruleTypeMatches(SecurityAlertEntity alert, String ruleType) {
        if (!StringUtils.hasText(alert.getAlertType())) {
            return false;
        }
        return occurrenceService.findByAlertId(alert.getId()).stream()
                .anyMatch(occurrence -> ruleType.trim().equalsIgnoreCase(occurrence.getRuleType()));
    }

    public SecurityAlertResponse toResponse(SecurityAlertEntity entity) {
        return SecurityAlertResponse.builder()
                .id(entity.getId()).alertType(entity.getAlertType()).severity(entity.getSeverity()).status(entity.getStatus())
                .fingerprint(entity.getFingerprint()).serviceName(entity.getServiceName()).endpointId(entity.getEndpointId())
                .endpointName(entity.getEndpointName()).flowType(entity.getFlowType()).protocol(entity.getProtocol())
                .clientId(entity.getClientId()).sourceIp(entity.getSourceIp()).resultCode(entity.getResultCode())
                .metric(entity.getMetric()).scopeType(entity.getScopeType()).currentValue(entity.getCurrentValue())
                .thresholdValue(entity.getThresholdValue()).windowSeconds(entity.getWindowSeconds()).count(entity.getCount())
                .message(entity.getMessage()).notificationRuleId(entity.getNotificationRuleId()).cooldownMinutes(entity.getCooldownMinutes()).firstSeenAt(entity.getFirstSeenAt())
                .lastSeenAt(entity.getLastSeenAt()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt())
                .acknowledgedBy(entity.getAcknowledgedBy()).acknowledgedAt(entity.getAcknowledgedAt())
                .ignoredBy(entity.getIgnoredBy()).ignoredAt(entity.getIgnoredAt()).ignoredUntil(entity.getIgnoredUntil())
                .resolvedBy(entity.getResolvedBy()).resolvedAt(entity.getResolvedAt())
                .build();
    }

    private SecurityAlertOccurrenceResponse toResponse(SecurityAlertOccurrenceEntity entity) {
        return SecurityAlertOccurrenceResponse.builder()
                .id(entity.getId()).alertId(entity.getAlertId()).ruleCode(entity.getRuleCode()).ruleType(entity.getRuleType())
                .metric(entity.getMetric()).scopeType(entity.getScopeType()).scopeKey(entity.getScopeKey())
                .currentValue(entity.getCurrentValue()).thresholdValue(entity.getThresholdValue()).baselineValue(entity.getBaselineValue())
                .windowSeconds(entity.getWindowSeconds()).windowStart(entity.getWindowStart()).windowEnd(entity.getWindowEnd())
                .timeBucket(entity.getTimeBucket()).eventTimestamp(entity.getEventTimestamp()).createdAt(entity.getCreatedAt())
                .build();
    }

    private NotificationDeliveryResponse toResponse(NotificationDeliveryEntity entity) {
        return NotificationDeliveryResponse.builder()
                .id(entity.getId()).alertId(entity.getAlertId()).notificationRuleId(entity.getNotificationRuleId())
                .channel(entity.getChannel()).recipient(entity.getRecipient()).status(entity.getStatus())
                .attemptCount(entity.getAttemptCount()).lastError(entity.getLastError()).sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt())
                .build();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
