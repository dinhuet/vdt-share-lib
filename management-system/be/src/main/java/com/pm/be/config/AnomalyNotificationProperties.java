package com.pm.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vdt.anomaly.notification")
public class AnomalyNotificationProperties {
    private Boolean enabled = true;
    private Integer cooldownDefaultMinutes = 15;
    private Channel email = new Channel(false);
    private Channel webhook = new Channel(false);
    private Channel sms = new Channel(false);
    private String dashboardUrl = "http://localhost:3000/security-alerts";

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getCooldownDefaultMinutes() { return cooldownDefaultMinutes; }
    public void setCooldownDefaultMinutes(Integer cooldownDefaultMinutes) { this.cooldownDefaultMinutes = cooldownDefaultMinutes; }
    public Channel getEmail() { return email; }
    public void setEmail(Channel email) { this.email = email; }
    public Channel getWebhook() { return webhook; }
    public void setWebhook(Channel webhook) { this.webhook = webhook; }
    public Channel getSms() { return sms; }
    public void setSms(Channel sms) { this.sms = sms; }
    public String getDashboardUrl() { return dashboardUrl; }
    public void setDashboardUrl(String dashboardUrl) { this.dashboardUrl = dashboardUrl; }

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
}
