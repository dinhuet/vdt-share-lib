package com.pm.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "vdt.anomaly.detector")
public class AnomalyDetectorProperties {
    private Boolean enabled = true;
    private String keyPrefix = "vdt:anomaly";
    private List<Integer> windows = new ArrayList<>(List.of(60, 300, 900));
    private List<Integer> successWindows = new ArrayList<>(List.of(60, 300));
    private List<Integer> violationWindows = new ArrayList<>(List.of(60, 300, 900));
    private Boolean successClientScopeEnabled = true;
    private Boolean successIpScopeEnabled = false;
    private Integer counterTtlMultiplier = 2;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public List<Integer> getWindows() {
        return windows;
    }

    public void setWindows(List<Integer> windows) {
        this.windows = windows;
    }

    public List<Integer> getSuccessWindows() {
        return successWindows;
    }

    public void setSuccessWindows(List<Integer> successWindows) {
        this.successWindows = successWindows;
    }

    public List<Integer> getViolationWindows() {
        return violationWindows;
    }

    public void setViolationWindows(List<Integer> violationWindows) {
        this.violationWindows = violationWindows;
    }

    public Boolean getSuccessClientScopeEnabled() {
        return successClientScopeEnabled;
    }

    public void setSuccessClientScopeEnabled(Boolean successClientScopeEnabled) {
        this.successClientScopeEnabled = successClientScopeEnabled;
    }

    public Boolean getSuccessIpScopeEnabled() {
        return successIpScopeEnabled;
    }

    public void setSuccessIpScopeEnabled(Boolean successIpScopeEnabled) {
        this.successIpScopeEnabled = successIpScopeEnabled;
    }

    public Integer getCounterTtlMultiplier() {
        return counterTtlMultiplier;
    }

    public void setCounterTtlMultiplier(Integer counterTtlMultiplier) {
        this.counterTtlMultiplier = counterTtlMultiplier;
    }
}
