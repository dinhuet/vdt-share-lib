package com.pm.be.service.anomaly;

import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificationSender implements NotificationChannelSender {
    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(SecurityAlertEntity alert, String recipient) {
        log.info("Email notification sender is running in logging mode: alertId={} recipient={} severity={}",
                alert == null ? null : alert.getId(), recipient, alert == null ? null : alert.getSeverity());
    }
}
