package com.pm.be.service.anomaly;

import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.NotificationChannel;
import org.springframework.stereotype.Service;

@Service
public class NotificationPolicyService {
    public boolean isChannelAllowed(SecurityAlertEntity alert, NotificationChannel channel) {
        if (alert == null || alert.getSeverity() == null) {
            return false;
        }
        if (channel == NotificationChannel.CENTRAL) {
            return alert.getSeverity().ordinal() >= AnomalySeverity.MEDIUM.ordinal();
        }
        if (channel == NotificationChannel.EMAIL) {
            return alert.getSeverity() == AnomalySeverity.HIGH || alert.getSeverity() == AnomalySeverity.CRITICAL;
        }
        return alert.getSeverity() == AnomalySeverity.CRITICAL;
    }

    public boolean shouldAutoBlacklist(SecurityAlertEntity alert) {
        return alert != null && alert.getSeverity() == AnomalySeverity.CRITICAL;
    }
}
