package com.pm.be.service.anomaly;

import com.pm.be.entity.anomaly.NotificationDeliveryEntity;
import com.pm.be.enums.NotificationChannel;
import com.pm.be.enums.NotificationDeliveryStatus;
import com.pm.be.repository.anomaly.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {
    private final NotificationDeliveryRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDeliveryEntity record(UUID alertId, UUID notificationRuleId, NotificationChannel channel,
                                             String recipient, NotificationDeliveryStatus status,
                                             int attemptCount, String lastError) {
        NotificationDeliveryEntity delivery = NotificationDeliveryEntity.builder()
                .alertId(alertId)
                .notificationRuleId(notificationRuleId)
                .channel(channel)
                .recipient(recipient == null || recipient.isBlank() ? "dashboard" : recipient.trim())
                .status(status)
                .attemptCount(attemptCount)
                .lastError(lastError)
                .sentAt(status == NotificationDeliveryStatus.SENT ? LocalDateTime.now() : null)
                .build();
        return repository.save(delivery);
    }

    public List<NotificationDeliveryEntity> findByAlertId(UUID alertId) {
        return repository.findByAlertIdOrderByCreatedAtDesc(alertId);
    }
}
