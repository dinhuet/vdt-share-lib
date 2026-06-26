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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {
    @Mock NotificationRuleResolver resolver;
    @Mock NotificationCooldownService cooldownService;
    @Mock NotificationDeliveryService deliveryService;
    @Mock NotificationChannelSender sender;
    @Mock SecurityAlertBlacklistService blacklistService;

    private AnomalyNotificationProperties properties;
    private NotificationPolicyService policyService;
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        properties = new AnomalyNotificationProperties();
        properties.getEmail().setEnabled(true);
        properties.getEmail().setTo("security@example.com");
        policyService = new NotificationPolicyService();
        dispatcher = new NotificationDispatcher(properties, resolver, cooldownService, deliveryService, policyService, blacklistService, List.of(sender), new ObjectMapper());
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
        when(resolver.resolve(alert)).thenReturn(Optional.empty());
        when(cooldownService.reserve(eq(null), eq(alert.getAlertType()), eq(alert.getFingerprint()), eq(NotificationChannel.CENTRAL), any(), eq(15))).thenReturn(true);
        when(cooldownService.reserve(eq(null), eq(alert.getAlertType()), eq(alert.getFingerprint()), eq(NotificationChannel.EMAIL), eq("security@example.com"), eq(15))).thenReturn(false);

        dispatcher.dispatch(alert);

        verify(deliveryService).record(eq(alert.getId()), eq(null), eq(NotificationChannel.EMAIL), eq("security@example.com"), eq(NotificationDeliveryStatus.SKIPPED_COOLDOWN), eq(0), any());
    }

    @Test
    void dispatch_highWithoutNotificationRule_shouldSendEmailToConfiguredRecipient() {
        SecurityAlertEntity alert = alert(AnomalySeverity.HIGH);
        when(resolver.resolve(alert)).thenReturn(Optional.empty());
        when(cooldownService.reserve(eq(null), eq(alert.getAlertType()), eq(alert.getFingerprint()), any(), any(), eq(15))).thenReturn(true);
        when(sender.supports(NotificationChannel.EMAIL)).thenReturn(true);

        dispatcher.dispatch(alert);

        verify(sender).send(alert, "security@example.com");
        verify(deliveryService).record(eq(alert.getId()), eq(null), eq(NotificationChannel.EMAIL), eq("security@example.com"), eq(NotificationDeliveryStatus.SENT), eq(1), eq(null));
    }

    @Test
    void dispatch_senderThrows_shouldRecordFailed() {
        SecurityAlertEntity alert = alert(AnomalySeverity.CRITICAL);
        when(resolver.resolve(alert)).thenReturn(Optional.empty());
        when(cooldownService.reserve(eq(null), eq(alert.getAlertType()), eq(alert.getFingerprint()), any(), any(), eq(15))).thenReturn(true);
        when(sender.supports(NotificationChannel.EMAIL)).thenReturn(true);
        doThrow(new IllegalStateException("SMTP down")).when(sender).send(alert, "security@example.com");

        dispatcher.dispatch(alert);

        verify(blacklistService).autoBlacklistCritical(alert);
        verify(deliveryService).record(eq(alert.getId()), eq(null), eq(NotificationChannel.EMAIL), eq("security@example.com"), eq(NotificationDeliveryStatus.FAILED), eq(1), eq("SMTP down"));
    }

    @Test
    void dispatch_medium_shouldNotEmail() {
        SecurityAlertEntity alert = alert(AnomalySeverity.MEDIUM);
        when(resolver.resolve(alert)).thenReturn(Optional.empty());
        when(cooldownService.reserve(eq(null), eq(alert.getAlertType()), eq(alert.getFingerprint()), eq(NotificationChannel.CENTRAL), any(), eq(15))).thenReturn(true);

        dispatcher.dispatch(alert);

        verify(sender, never()).send(any(), any());
        verify(deliveryService, never()).record(eq(alert.getId()), eq(null), eq(NotificationChannel.EMAIL), any(), any(), any(Integer.class), any());
    }

    @Test
    void dispatch_blankEmailRecipient_shouldRecordSkippedDisabledAndNotCallSender() {
        SecurityAlertEntity alert = alert(AnomalySeverity.HIGH);
        properties.getEmail().setTo(" ");
        when(resolver.resolve(alert)).thenReturn(Optional.empty());
        when(cooldownService.reserve(eq(null), eq(alert.getAlertType()), eq(alert.getFingerprint()), eq(NotificationChannel.CENTRAL), any(), eq(15))).thenReturn(true);

        dispatcher.dispatch(alert);

        verify(sender, never()).send(any(), any());
        verify(deliveryService).record(eq(alert.getId()), eq(null), eq(NotificationChannel.EMAIL), eq(null), eq(NotificationDeliveryStatus.SKIPPED_DISABLED), eq(0), eq("Email recipient vdt.anomaly.notification.email.to is not configured"));
    }

    @Test
    void dispatch_alertCooldownPresent_shouldUseAlertCooldown() {
        SecurityAlertEntity alert = alert(AnomalySeverity.HIGH);
        alert.setCooldownMinutes(7);
        when(resolver.resolve(alert)).thenReturn(Optional.empty());
        when(cooldownService.reserve(eq(null), eq(alert.getAlertType()), eq(alert.getFingerprint()), any(), any(), eq(7))).thenReturn(true);
        when(sender.supports(NotificationChannel.EMAIL)).thenReturn(true);

        dispatcher.dispatch(alert);

        verify(cooldownService).reserve(eq(null), eq(alert.getAlertType()), eq(alert.getFingerprint()), eq(NotificationChannel.EMAIL), eq("security@example.com"), eq(7));
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
