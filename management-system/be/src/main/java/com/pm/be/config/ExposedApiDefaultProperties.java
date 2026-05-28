package com.pm.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.exposed.default")
public class ExposedApiDefaultProperties {
    private Integer maxRequests = 100;
    private Integer throttleWindowSec = 60;
    private Integer maxRequestKb = 1024;
    private Integer maxResponseKb = 2048;
    private Integer latencyThresholdMs = 1000;
    private Integer timeoutMs = 30000;
    private Integer logRetentionDays = 30;
    private Boolean enabled = true;

    public Integer getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(Integer maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Integer getThrottleWindowSec() {
        return throttleWindowSec;
    }

    public void setThrottleWindowSec(Integer throttleWindowSec) {
        this.throttleWindowSec = throttleWindowSec;
    }

    public Integer getMaxRequestKb() {
        return maxRequestKb;
    }

    public void setMaxRequestKb(Integer maxRequestKb) {
        this.maxRequestKb = maxRequestKb;
    }

    public Integer getMaxResponseKb() {
        return maxResponseKb;
    }

    public void setMaxResponseKb(Integer maxResponseKb) {
        this.maxResponseKb = maxResponseKb;
    }

    public Integer getLatencyThresholdMs() {
        return latencyThresholdMs;
    }

    public void setLatencyThresholdMs(Integer latencyThresholdMs) {
        this.latencyThresholdMs = latencyThresholdMs;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getLogRetentionDays() {
        return logRetentionDays;
    }

    public void setLogRetentionDays(Integer logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
