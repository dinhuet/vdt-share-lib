package com.pm.be.dto.response.anomaly;

import com.pm.be.enums.NotificationChannel;
import com.pm.be.enums.NotificationDeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationDeliveryResponse {
    private UUID id;
    private UUID alertId;
    private UUID notificationRuleId;
    private NotificationChannel channel;
    private String recipient;
    private NotificationDeliveryStatus status;
    private Integer attemptCount;
    private String lastError;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
