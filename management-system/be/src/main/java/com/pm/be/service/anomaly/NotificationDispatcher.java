package com.pm.be.service.anomaly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.config.AnomalyNotificationProperties;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.entity.exposedapi.NotificationRuleEntity;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.NotificationChannel;
import com.pm.be.enums.NotificationDeliveryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {
    private final AnomalyNotificationProperties properties;
    private final NotificationRuleResolver ruleResolver;
    private final NotificationCooldownService cooldownService;
    private final NotificationDeliveryService deliveryService;
    private final List<NotificationChannelSender> senders;
    private final ObjectMapper objectMapper;

    public void dispatch(SecurityAlertEntity alert) {
        if (alert == null || alert.getId() == null) {
            return;
        }
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            deliveryService.record(alert.getId(), alert.getNotificationRuleId(), NotificationChannel.CENTRAL,
                    properties.getDashboardUrl(), NotificationDeliveryStatus.SKIPPED_DISABLED, 0, "Notification disabled");
            return;
        }

        Optional<NotificationRuleEntity> rule = ruleResolver.resolve(alert);
        if (rule.isPresent() && !Boolean.TRUE.equals(rule.get().getEnabled())) {
            deliveryService.record(alert.getId(), rule.get().getId(), NotificationChannel.CENTRAL,
                    properties.getDashboardUrl(), NotificationDeliveryStatus.SKIPPED_DISABLED, 0, "Notification rule disabled");
            return;
        }

        UUID ruleId = rule.map(NotificationRuleEntity::getId).orElse(null);
        int cooldownMinutes = rule.map(NotificationRuleEntity::getCooldownMinutes)
                .filter(value -> value != null && value > 0)
                .orElseGet(() -> properties.getCooldownDefaultMinutes() == null ? 15 : properties.getCooldownDefaultMinutes());
        if (isBelowSeverity(alert.getSeverity(), rule.map(NotificationRuleEntity::getSeverity).orElse(null))) {
            deliveryService.record(alert.getId(), ruleId, NotificationChannel.CENTRAL,
                    properties.getDashboardUrl(), NotificationDeliveryStatus.SKIPPED_SEVERITY, 0, "Severity below notification rule threshold");
            return;
        }

        recordCentral(alert, ruleId, cooldownMinutes);
        for (String recipient : emailRecipients(rule.map(NotificationRuleEntity::getRecipients).orElse(null))) {
            dispatchChannel(alert, ruleId, NotificationChannel.EMAIL, recipient, cooldownMinutes, properties.isEmailEnabled());
        }
        for (String recipient : channelRecipients(rule.map(NotificationRuleEntity::getRecipients).orElse(null), "webhook", "webhooks")) {
            dispatchChannel(alert, ruleId, NotificationChannel.WEBHOOK, recipient, cooldownMinutes, properties.isWebhookEnabled());
        }
        for (String recipient : channelRecipients(rule.map(NotificationRuleEntity::getRecipients).orElse(null), "sms", "phones")) {
            dispatchChannel(alert, ruleId, NotificationChannel.SMS, recipient, cooldownMinutes, properties.isSmsEnabled());
        }
    }

    private void recordCentral(SecurityAlertEntity alert, UUID ruleId, int cooldownMinutes) {
        if (!cooldownService.reserve(ruleId, alert.getAlertType(), alert.getFingerprint(), NotificationChannel.CENTRAL, properties.getDashboardUrl(), cooldownMinutes)) {
            deliveryService.record(alert.getId(), ruleId, NotificationChannel.CENTRAL, properties.getDashboardUrl(), NotificationDeliveryStatus.SKIPPED_COOLDOWN, 0, null);
            return;
        }
        deliveryService.record(alert.getId(), ruleId, NotificationChannel.CENTRAL, properties.getDashboardUrl(), NotificationDeliveryStatus.SENT, 0, null);
    }

    private void dispatchChannel(SecurityAlertEntity alert, UUID ruleId, NotificationChannel channel, String recipient, int cooldownMinutes, boolean enabled) {
        if (!enabled) {
            deliveryService.record(alert.getId(), ruleId, channel, recipient, NotificationDeliveryStatus.SKIPPED_DISABLED, 0, "Channel disabled");
            return;
        }
        if (channel == NotificationChannel.EMAIL && !isEmailSeverity(alert.getSeverity())) {
            deliveryService.record(alert.getId(), ruleId, channel, recipient, NotificationDeliveryStatus.SKIPPED_SEVERITY, 0, "Email requires HIGH or CRITICAL severity");
            return;
        }
        if (!cooldownService.reserve(ruleId, alert.getAlertType(), alert.getFingerprint(), channel, recipient, cooldownMinutes)) {
            deliveryService.record(alert.getId(), ruleId, channel, recipient, NotificationDeliveryStatus.SKIPPED_COOLDOWN, 0, null);
            return;
        }
        try {
            sender(channel).send(alert, recipient);
            deliveryService.record(alert.getId(), ruleId, channel, recipient, NotificationDeliveryStatus.SENT, 1, null);
        } catch (RuntimeException e) {
            log.warn("Notification send failed: alertId={} channel={} recipient={}", alert.getId(), channel, recipient, e);
            deliveryService.record(alert.getId(), ruleId, channel, recipient, NotificationDeliveryStatus.FAILED, 1, e.getMessage());
        }
    }

    private NotificationChannelSender sender(NotificationChannel channel) {
        return senders.stream()
                .filter(sender -> sender.supports(channel))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No notification sender for channel " + channel));
    }

    private boolean isEmailSeverity(AnomalySeverity severity) {
        return severity == AnomalySeverity.HIGH || severity == AnomalySeverity.CRITICAL;
    }

    private boolean isBelowSeverity(AnomalySeverity severity, String threshold) {
        if (!StringUtils.hasText(threshold) || "WARNING".equalsIgnoreCase(threshold.trim())) {
            return false;
        }
        try {
            AnomalySeverity min = AnomalySeverity.valueOf(threshold.trim().toUpperCase());
            return severity != null && severity.ordinal() < min.ordinal();
        } catch (IllegalArgumentException e) {
            log.warn("Unknown notification severity threshold {}; rule will be treated as enabled for all severities", threshold);
            return false;
        }
    }

    private List<String> emailRecipients(String recipientsJson) {
        return channelRecipients(recipientsJson, "email", "emails");
    }

    private List<String> channelRecipients(String recipientsJson, String singular, String plural) {
        List<String> recipients = new ArrayList<>();
        if (!StringUtils.hasText(recipientsJson)) {
            return recipients;
        }
        try {
            JsonNode root = objectMapper.readTree(recipientsJson);
            collect(root.get(singular), recipients);
            collect(root.get(plural), recipients);
            if (root.isArray()) {
                collect(root, recipients);
            }
        } catch (Exception e) {
            log.warn("Failed to parse notification recipients JSON", e);
        }
        return recipients.stream().filter(StringUtils::hasText).distinct().toList();
    }

    private void collect(JsonNode node, List<String> values) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> collect(item, values));
        } else if (node.isTextual()) {
            values.add(node.asText());
        }
    }
}
