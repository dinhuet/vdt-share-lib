package com.pm.be.service.anomaly;

import com.pm.be.dto.request.anomaly.SecurityAlertActionRequest;
import com.pm.be.entity.anomaly.SecurityAlertActionEntity;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.SecurityAlertActionTargetType;
import com.pm.be.enums.SecurityAlertActionType;
import com.pm.be.enums.SecurityAlertStatus;
import com.pm.be.exception.AppException;
import com.pm.be.exception.ErrorCode;
import com.pm.be.repository.anomaly.SecurityAlertActionRepository;
import com.pm.be.repository.anomaly.NotificationDeliveryRepository;
import com.pm.be.repository.anomaly.SecurityAlertOccurrenceRepository;
import com.pm.be.repository.anomaly.SecurityAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityAlertActionService {
    private final SecurityAlertRepository alertRepository;
    private final SecurityAlertActionRepository actionRepository;
    private final SecurityAlertOccurrenceRepository occurrenceRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;

    @Transactional
    public SecurityAlertEntity ack(UUID alertId, SecurityAlertActionRequest request) {
        SecurityAlertEntity alert = getAlert(alertId);
        String user = resolveCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        alert.setStatus(SecurityAlertStatus.ACKED);
        alert.setAcknowledgedBy(user);
        alert.setAcknowledgedAt(now);
        SecurityAlertEntity saved = alertRepository.save(alert);
        record(alertId, SecurityAlertActionType.ACK, request, user);
        return saved;
    }

    @Transactional
    public SecurityAlertEntity ignore(UUID alertId, SecurityAlertActionRequest request) {
        SecurityAlertEntity alert = getAlert(alertId);
        String user = resolveCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        alert.setStatus(SecurityAlertStatus.IGNORED);
        alert.setIgnoredBy(user);
        alert.setIgnoredAt(now);
        alert.setIgnoredUntil(toLocalDateTime(request == null ? null : request.getIgnoredUntil()));
        SecurityAlertEntity saved = alertRepository.save(alert);
        record(alertId, SecurityAlertActionType.IGNORE, request, user);
        return saved;
    }

    @Transactional
    public SecurityAlertEntity resolve(UUID alertId, SecurityAlertActionRequest request) {
        SecurityAlertEntity alert = getAlert(alertId);
        String user = resolveCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        alert.setStatus(SecurityAlertStatus.RESOLVED);
        alert.setResolvedBy(user);
        alert.setResolvedAt(now);
        SecurityAlertEntity saved = alertRepository.save(alert);
        record(alertId, SecurityAlertActionType.RESOLVE, request, user);
        return saved;
    }

    @Transactional
    public void delete(UUID alertId) {
        SecurityAlertEntity alert = getAlert(alertId);
        if (alert.getStatus() != SecurityAlertStatus.RESOLVED && alert.getStatus() != SecurityAlertStatus.IGNORED) {
            throw new AppException(ErrorCode.SECURITY_ALERT_DELETE_NOT_ALLOWED);
        }
        notificationDeliveryRepository.deleteByAlertId(alertId);
        occurrenceRepository.deleteByAlertId(alertId);
        actionRepository.deleteByAlertId(alertId);
        alertRepository.delete(alert);
    }

    private SecurityAlertEntity getAlert(UUID alertId) {
        return alertRepository.findById(alertId).orElseThrow(() -> new AppException(ErrorCode.SECURITY_ALERT_NOTFOUND));
    }

    private void record(UUID alertId, SecurityAlertActionType actionType, SecurityAlertActionRequest request, String user) {
        actionRepository.save(SecurityAlertActionEntity.builder()
                .alertId(alertId)
                .actionType(actionType)
                .targetType(SecurityAlertActionTargetType.NONE)
                .reason(request == null ? null : trimToNull(request.getReason()))
                .createdBy(user)
                .build());
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

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
