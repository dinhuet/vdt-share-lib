package com.pm.be.service.anomaly;

import com.pm.be.config.StaticRuleProperties;
import com.pm.be.dto.anomaly.AnomalyRuleMatch;
import com.pm.be.dto.anomaly.SecurityAnomalyEvent;
import com.pm.be.dto.anomaly.SecurityLogEventMessage;
import com.pm.be.dto.anomaly.StaticRuleMatch;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.SecurityAlertStatus;
import com.pm.be.repository.anomaly.SecurityAlertRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAlertService {
    private final SecurityAlertRepository securityAlertRepository;
    private final StaticRuleProperties properties;
    private final SecurityAlertOccurrenceService occurrenceService;
    private final NotificationDispatcher notificationDispatcher;

    @Transactional
    public SecurityAnomalyEvent createOrUpdate(StaticRuleMatch match) {
        return createOrUpdate(AnomalyRuleMatch.fromStatic(match));
    }

    @Transactional
    public SecurityAnomalyEvent createOrUpdate(AnomalyRuleMatch match) {
        String fingerprint = buildFingerprint(match);
        List<SecurityAlertStatus> openStatuses = openStatuses();
        SecurityAlertEntity alert = securityAlertRepository.findFirstByFingerprintAndStatusIn(fingerprint, openStatuses)
                .map(existing -> updateExisting(existing, match))
                .orElseGet(() -> createNew(match, fingerprint));
        SecurityAlertEntity saved = securityAlertRepository.save(alert);
        createOccurrence(saved, match);
        dispatchNotification(saved);
        return toEvent(saved, match);
    }

    private void createOccurrence(SecurityAlertEntity alert, AnomalyRuleMatch match) {
        try {
            occurrenceService.createIfAllowed(alert, match);
        } catch (RuntimeException e) {
            log.warn("Failed to create security alert occurrence; alert remains persisted: alertId={} ruleCode={}",
                    alert == null ? null : alert.getId(), match == null ? null : match.ruleCode(), e);
        }
    }

    private void dispatchNotification(SecurityAlertEntity alert) {
        try {
            notificationDispatcher.dispatch(alert);
        } catch (RuntimeException e) {
            log.warn("Failed to dispatch security alert notification; alert remains persisted: alertId={}",
                    alert == null ? null : alert.getId(), e);
        }
    }

    public String buildFingerprint(AnomalyRuleMatch match) {
        SecurityLogEventMessage event = match.event();
        String flowType = StringUtils.hasText(event.getFlowType()) ? event.getFlowType().trim().toUpperCase(Locale.ROOT) : "UNKNOWN";
        String endpointId = StringUtils.hasText(event.getEndpointId()) ? event.getEndpointId().trim() : "none";
        return match.ruleCode() + ":" + flowType + ":" + endpointId + ":" + match.scopeType().name() + ":" + match.identity();
    }

    public String buildFingerprint(StaticRuleMatch match) {
        return buildFingerprint(AnomalyRuleMatch.fromStatic(match));
    }

    private SecurityAlertEntity createNew(AnomalyRuleMatch match, String fingerprint) {
        SecurityLogEventMessage event = match.event();
        return SecurityAlertEntity.builder()
                .alertType(match.ruleCode())
                .severity(match.severity())
                .status(SecurityAlertStatus.OPEN)
                .fingerprint(fingerprint)
                .serviceName(trim(event.getServiceName()))
                .endpointId(trim(event.getEndpointId()))
                .endpointName(trim(event.getEndpointName()))
                .flowType(trim(event.getFlowType()))
                .protocol(trim(event.getProtocol()))
                .clientId(trim(event.getClientId()))
                .sourceIp(trim(event.getSourceIp()))
                .resultCode(resultCode(event))
                .metric(match.metric())
                .scopeType(match.scopeType())
                .currentValue(match.currentValue())
                .thresholdValue(match.thresholdValue())
                .windowSeconds(match.windowSeconds())
                .count(match.currentValue().longValue())
                .message(buildMessage(match))
                .notificationRuleId(match.notificationRuleId())
                .firstSeenAt(toLocalDateTime(match.event().getTimestamp()))
                .lastSeenAt(toLocalDateTime(match.event().getTimestamp()))
                .build();
    }

    private SecurityAlertEntity updateExisting(SecurityAlertEntity alert, AnomalyRuleMatch match) {
        alert.setSeverity(maxSeverity(alert.getSeverity(), match.severity()));
        alert.setCurrentValue(max(alert.getCurrentValue(), match.currentValue()));
        alert.setThresholdValue(match.thresholdValue());
        alert.setWindowSeconds(match.windowSeconds());
        alert.setCount(match.currentValue().longValue());
        alert.setMessage(buildMessage(match));
        alert.setLastSeenAt(toLocalDateTime(match.event().getTimestamp()));
        alert.setResultCode(resultCode(match.event()));
        return alert;
    }

    private SecurityAnomalyEvent toEvent(SecurityAlertEntity alert, AnomalyRuleMatch match) {
        SecurityLogEventMessage event = match.event();
        return SecurityAnomalyEvent.builder()
                .timestamp(Instant.now())
                .alertId(alert.getId())
                .eventType("ANOMALY_ALERT")
                .ruleCode(match.ruleCode())
                .ruleType(match.ruleType())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .fingerprint(alert.getFingerprint())
                .serviceName(alert.getServiceName())
                .endpointId(alert.getEndpointId())
                .endpointName(alert.getEndpointName())
                .flowType(alert.getFlowType())
                .protocol(alert.getProtocol())
                .clientId(alert.getClientId())
                .sourceIp(alert.getSourceIp())
                .resultCode(resultCode(event))
                .metric(match.metric())
                .scopeType(match.scopeType())
                .scopeKey(match.scopeKey())
                .currentValue(match.currentValue())
                .baselineValue(match.baselineValue())
                .thresholdValue(match.thresholdValue())
                .windowSeconds(match.windowSeconds())
                .timeBucket(match.timeBucket())
                .staticRuleCode(match.staticRuleCode())
                .baselineRuleCode(match.baselineRuleCode())
                .windowStart(match.windowStart())
                .windowEnd(match.windowEnd())
                .message(alert.getMessage())
                .build();
    }

    private List<SecurityAlertStatus> openStatuses() {
        if (properties.getAlertOpenStatuses() == null || properties.getAlertOpenStatuses().isEmpty()) {
            return List.of(SecurityAlertStatus.OPEN, SecurityAlertStatus.ACKED);
        }
        return properties.getAlertOpenStatuses().stream()
                .filter(StringUtils::hasText)
                .map(status -> SecurityAlertStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)))
                .toList();
    }

    private String buildMessage(AnomalyRuleMatch match) {
        if ("BASELINE".equals(match.ruleType()) || "HYBRID".equals(match.ruleType())) {
            return match.ruleCode()
                    + " matched: "
                    + match.metric()
                    + "="
                    + match.currentValue().stripTrailingZeros().toPlainString()
                    + " exceeded dynamic baseline threshold "
                    + match.thresholdValue().stripTrailingZeros().toPlainString();
        }
        return match.ruleCode()
                + " matched: "
                + match.metric()
                + "="
                + match.currentValue().stripTrailingZeros().toPlainString()
                + " "
                + match.scopeType().name()
                + " within "
                + match.windowSeconds()
                + " seconds (threshold "
                + match.thresholdValue().stripTrailingZeros().toPlainString()
                + ")";
    }

    private AnomalySeverity maxSeverity(AnomalySeverity current, AnomalySeverity incoming) {
        if (current == null) {
            return incoming;
        }
        if (incoming == null) {
            return current;
        }
        return incoming.ordinal() > current.ordinal() ? incoming : current;
    }

    private BigDecimal max(BigDecimal current, BigDecimal incoming) {
        if (current == null) {
            return incoming;
        }
        return current.compareTo(incoming) >= 0 ? current : incoming;
    }

    private String resultCode(SecurityLogEventMessage event) {
        return StringUtils.hasText(event.getResultCode()) ? event.getResultCode().trim() : trim(event.getErrorCode());
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? LocalDateTime.now() : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
