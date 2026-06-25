package com.pm.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vdt.anomaly.occurrence")
public class AnomalyOccurrenceProperties {
    private Boolean enabled = true;
    private Boolean cooldownEnabled = true;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getCooldownEnabled() { return cooldownEnabled; }
    public void setCooldownEnabled(Boolean cooldownEnabled) { this.cooldownEnabled = cooldownEnabled; }
}
