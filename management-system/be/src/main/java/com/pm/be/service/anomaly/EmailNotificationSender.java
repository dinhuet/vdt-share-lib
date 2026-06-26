package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyNotificationProperties;
import com.pm.be.entity.anomaly.SecurityAlertEntity;
import com.pm.be.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationChannelSender {
    private final JavaMailSender mailSender;
    private final AnomalyNotificationProperties properties;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(SecurityAlertEntity alert, String recipient) {
        if (alert == null || alert.getId() == null || !StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Alert and recipient are required for email notification");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getEmail().getFrom());
        message.setTo(recipient.trim());
        message.setSubject(buildSubject(alert));
        message.setText(buildBody(alert));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new IllegalStateException("Failed to send alert email", e);
        }
    }

    private String buildSubject(SecurityAlertEntity alert) {
        String prefix = StringUtils.hasText(properties.getEmail().getSubjectPrefix())
                ? properties.getEmail().getSubjectPrefix().trim() + " "
                : "";
        String severity = alert.getSeverity() == null ? "UNKNOWN" : alert.getSeverity().name();
        String target = StringUtils.hasText(alert.getEndpointName()) ? alert.getEndpointName() : alert.getEndpointId();
        if (StringUtils.hasText(alert.getServiceName()) && StringUtils.hasText(target)) {
            target = alert.getServiceName() + "/" + target;
        } else if (StringUtils.hasText(alert.getServiceName())) {
            target = alert.getServiceName();
        }
        return prefix + "[" + severity + "] " + nullSafe(alert.getAlertType()) + " detected on " + nullSafe(target);
    }

    private String buildBody(SecurityAlertEntity alert) {
        String dashboardLink = properties.getDashboardUrl();
        if (StringUtils.hasText(dashboardLink)) {
            dashboardLink = dashboardLink.trim() + "/" + alert.getId();
        }
        return "Security anomaly alert\n\n"
                + "Alert ID: " + alert.getId() + "\n"
                + "Severity: " + nullSafe(alert.getSeverity()) + "\n"
                + "Alert type: " + nullSafe(alert.getAlertType()) + "\n"
                + "Service: " + nullSafe(alert.getServiceName()) + "\n"
                + "Endpoint: " + nullSafe(alert.getEndpointName()) + " (" + nullSafe(alert.getEndpointId()) + ")\n"
                + "Flow/protocol: " + nullSafe(alert.getFlowType()) + "/" + nullSafe(alert.getProtocol()) + "\n"
                + "Client ID: " + nullSafe(alert.getClientId()) + "\n"
                + "Source IP: " + nullSafe(alert.getSourceIp()) + "\n"
                + "Metric: " + nullSafe(alert.getMetric()) + " current=" + nullSafe(alert.getCurrentValue())
                + " threshold=" + nullSafe(alert.getThresholdValue()) + " windowSeconds=" + nullSafe(alert.getWindowSeconds()) + "\n"
                + "First seen: " + nullSafe(alert.getFirstSeenAt()) + "\n"
                + "Last seen: " + nullSafe(alert.getLastSeenAt()) + "\n"
                + "Message: " + nullSafe(alert.getMessage()) + "\n"
                + "Dashboard: " + nullSafe(dashboardLink) + "\n\n"
                + "Suggested action: " + suggestedAction(alert);
    }

    private String suggestedAction(SecurityAlertEntity alert) {
        if (alert.getSeverity() != null && "CRITICAL".equals(alert.getSeverity().name())) {
            return "Auto temporary blacklist was attempted. Review the target and resolve or extend the policy if needed.";
        }
        return "Review the alert, acknowledge/ignore/resolve it, and optionally apply a temporary blacklist.";
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : value.toString();
    }
}
