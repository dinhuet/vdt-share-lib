package com.pm.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "vdt.anomaly.static-rules")
public class StaticRuleProperties {
    private Boolean enabled = true;
    private Integer ruleCacheTtlSeconds = 60;
    private List<String> alertOpenStatuses = new ArrayList<>(List.of("OPEN", "ACKED"));
    private String anomalyTopic = "security.anomalies";
    private Boolean publishEnabled = true;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getRuleCacheTtlSeconds() {
        return ruleCacheTtlSeconds;
    }

    public void setRuleCacheTtlSeconds(Integer ruleCacheTtlSeconds) {
        this.ruleCacheTtlSeconds = ruleCacheTtlSeconds;
    }

    public List<String> getAlertOpenStatuses() {
        return alertOpenStatuses;
    }

    public void setAlertOpenStatuses(List<String> alertOpenStatuses) {
        this.alertOpenStatuses = alertOpenStatuses;
    }

    public String getAnomalyTopic() {
        return anomalyTopic;
    }

    public void setAnomalyTopic(String anomalyTopic) {
        this.anomalyTopic = anomalyTopic;
    }

    public Boolean getPublishEnabled() {
        return publishEnabled;
    }

    public void setPublishEnabled(Boolean publishEnabled) {
        this.publishEnabled = publishEnabled;
    }
}
