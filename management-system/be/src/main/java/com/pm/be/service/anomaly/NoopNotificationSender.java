package com.pm.be.service.anomaly;

import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NoopNotificationSender implements NotificationChannelSender {
    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SMS || channel == NotificationChannel.WEBHOOK || channel == NotificationChannel.CENTRAL;
    }

    @Override
    public void send(SecurityAlertEntity alert, String recipient) {
        log.debug("No-op notification sender: alertId={} recipient={}", alert == null ? null : alert.getId(), recipient);
    }
}
