package com.pm.be.service.anomaly;

import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.NotificationChannel;

public interface NotificationChannelSender {
    boolean supports(NotificationChannel channel);

    void send(SecurityAlertEntity alert, String recipient);
}
