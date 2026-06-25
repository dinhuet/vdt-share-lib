package com.pm.be.service.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.config.AnomalyNotificationProperties;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.entity.exposedapi.NotificationRuleEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.NotificationChannel;
import com.pm.be.enums.NotificationDeliveryStatus;
import com.pm.be.enums.SecurityAlertStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {
    @Mock NotificationRuleResolver resolver;
    @Mock NotificationCooldownService cooldownService;
    @Mock NotificationDeliveryService deliveryService;
    @Mock NotificationChannelSender sender;

    private AnomalyNotificationProperties properties;
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        properties = new AnomalyNotificationProperties();
        properties.getEmail().setEnabled(true);
        dispatcher = new NotificationDispatcher(properties, resolver, cooldownService, deliveryService, List.of(sender), new ObjectMapper());
    }

    @Test
    void dispatch_ruleDisabled_shouldRecordSkippedDisabled() {
        SecurityAlertEntity alert = alert(AnomalySeverity.HIGH);
        UUID ruleId = UUID.randomUUID();
        when(resolver.resolve(alert)).thenReturn(Optional.of(NotificationRuleEntity.builder().id(ruleId).enabled(false).build()));

        dispatcher.dispatch(alert);

        verify(deliveryService).record(eq(alert.getId()), eq(ruleId), eq(NotificationChannel.CENTRAL), any(), eq(NotificationDeliveryStatus.SKIPPED_DISABLED), eq(0), any());
    }

    @Test
    void dispatch_emailCooldown_shouldRecordSkippedCooldown() {
        SecurityAlertEntity alert = alert(AnomalySeverity.HIGH);
        UUID ruleId = UUID.randomUUID();
        NotificationRuleEntity rule = NotificationRuleEntity.builder()
                .id(ruleId).enabled(true).cooldownMinutes(15).recipients("{\"emails\":[\"admin@example.com\"]}").build();
        when(resolver.resolve(alert)).thenReturn(Optional.of(rule));
        when(cooldownService.reserve(eq(ruleId), eq(alert.getAlertType()), eq(alert.getFingerprint()), eq(NotificationChannel.CENTRAL), any(), eq(15))).thenReturn(true);
        when(cooldownService.reserve(eq(ruleId), eq(alert.getAlertType()), eq(alert.getFingerprint()), eq(NotificationChannel.EMAIL), eq("admin@example.com"), eq(15))).thenReturn(false);

        dispatcher.dispatch(alert);

        verify(deliveryService).record(eq(alert.getId()), eq(ruleId), eq(NotificationChannel.EMAIL), eq("admin@example.com"), eq(NotificationDeliveryStatus.SKIPPED_COOLDOWN), eq(0), any());
    }

    @Test
    void dispatch_senderThrows_shouldRecordFailed() {
        SecurityAlertEntity alert = alert(AnomalySeverity.CRITICAL);
        UUID ruleId = UUID.randomUUID();
        NotificationRuleEntity rule = NotificationRuleEntity.builder()
                .id(ruleId).enabled(true).cooldownMinutes(15).recipients("{\"emails\":[\"admin@example.com\"]}").build();
        when(resolver.resolve(alert)).thenReturn(Optional.of(rule));
        when(cooldownService.reserve(eq(ruleId), eq(alert.getAlertType()), eq(alert.getFingerprint()), any(), any(), eq(15))).thenReturn(true);
        when(sender.supports(NotificationChannel.EMAIL)).thenReturn(true);
        doThrow(new IllegalStateException("SMTP down")).when(sender).send(alert, "admin@example.com");

        dispatcher.dispatch(alert);

        verify(deliveryService).record(eq(alert.getId()), eq(ruleId), eq(NotificationChannel.EMAIL), eq("admin@example.com"), eq(NotificationDeliveryStatus.FAILED), eq(1), eq("SMTP down"));
    }

    private SecurityAlertEntity alert(AnomalySeverity severity) {
        return SecurityAlertEntity.builder()
                .id(UUID.randomUUID())
                .alertType("AUTH_BRUTE_FORCE")
                .severity(severity)
                .status(SecurityAlertStatus.OPEN)
                .fingerprint("fp")
                .metric("auth_fail_count")
                .scopeType(AnomalyScopeType.ENDPOINT_CLIENT)
                .currentValue(BigDecimal.TEN)
                .thresholdValue(BigDecimal.ONE)
                .windowSeconds(60)
                .count(10L)
                .build();
    }
}
