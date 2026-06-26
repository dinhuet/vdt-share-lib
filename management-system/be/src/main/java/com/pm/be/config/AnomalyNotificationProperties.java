package com.pm.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vdt.anomaly.notification")
public class AnomalyNotificationProperties {
    private Boolean enabled = true;
    private Integer cooldownDefaultMinutes = 15;
    private EmailChannel email = new EmailChannel(false);
    private Channel webhook = new Channel(false);
    private Channel sms = new Channel(false);
    private String dashboardUrl = "http://localhost:3000/security-alerts";
    private CriticalAutoBlacklist criticalAutoBlacklist = new CriticalAutoBlacklist();

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getCooldownDefaultMinutes() { return cooldownDefaultMinutes; }
    public void setCooldownDefaultMinutes(Integer cooldownDefaultMinutes) { this.cooldownDefaultMinutes = cooldownDefaultMinutes; }
    public EmailChannel getEmail() { return email; }
    public void setEmail(EmailChannel email) { this.email = email; }
    public Channel getWebhook() { return webhook; }
    public void setWebhook(Channel webhook) { this.webhook = webhook; }
    public Channel getSms() { return sms; }
    public void setSms(Channel sms) { this.sms = sms; }
    public String getDashboardUrl() { return dashboardUrl; }
    public void setDashboardUrl(String dashboardUrl) { this.dashboardUrl = dashboardUrl; }
    public CriticalAutoBlacklist getCriticalAutoBlacklist() { return criticalAutoBlacklist; }
    public void setCriticalAutoBlacklist(CriticalAutoBlacklist criticalAutoBlacklist) { this.criticalAutoBlacklist = criticalAutoBlacklist; }

    public boolean isEmailEnabled() { return email != null && Boolean.TRUE.equals(email.getEnabled()); }
    public boolean isWebhookEnabled() { return webhook != null && Boolean.TRUE.equals(webhook.getEnabled()); }
    public boolean isSmsEnabled() { return sms != null && Boolean.TRUE.equals(sms.getEnabled()); }

    public static class Channel {
        private Boolean enabled;

        public Channel() {
        }

        public Channel(Boolean enabled) {
            this.enabled = enabled;
        }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public static class EmailChannel extends Channel {
        private String to;
        private String from = "no-reply@vdt.local";
        private String subjectPrefix = "[VDT Security Alert]";

        public EmailChannel() {
            super();
        }

        public EmailChannel(Boolean enabled) {
            super(enabled);
        }

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getSubjectPrefix() { return subjectPrefix; }
        public void setSubjectPrefix(String subjectPrefix) { this.subjectPrefix = subjectPrefix; }
    }

    public static class CriticalAutoBlacklist {
        private Boolean enabled = true;
        private Integer durationMinutes = 15;
        private Boolean preferClient = true;

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public Boolean getPreferClient() { return preferClient; }
        public void setPreferClient(Boolean preferClient) { this.preferClient = preferClient; }
    }
}
