package com.pm.be.service.anomaly;

import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.entity.exposedapi.NotificationRuleEntity;
import com.pm.be.repository.exposedapi.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationRuleResolver {
    private final NotificationRuleRepository repository;

    public Optional<NotificationRuleEntity> resolve(SecurityAlertEntity alert) {
        if (alert == null || alert.getNotificationRuleId() == null) {
            return Optional.empty();
        }
        return repository.findById(alert.getNotificationRuleId());
    }
}
