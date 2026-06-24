package com.pm.be.config;

import com.pm.be.enums.AnomalyTimeBucketType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vdt.anomaly.baseline-rules")
public class BaselineRuleProperties {
    private Boolean enabled = true;
    private Integer ruleCacheTtlSeconds = 60;
    private Boolean baselineCacheEnabled = true;
    private Integer maxBaselineAgeHours = 2;
    private AnomalyTimeBucketType defaultTimeBucketType = AnomalyTimeBucketType.SAME_HOUR;
    private Boolean fallbackGlobalTimeBucket = true;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getRuleCacheTtlSeconds() { return ruleCacheTtlSeconds; }
    public void setRuleCacheTtlSeconds(Integer ruleCacheTtlSeconds) { this.ruleCacheTtlSeconds = ruleCacheTtlSeconds; }
    public Boolean getBaselineCacheEnabled() { return baselineCacheEnabled; }
    public void setBaselineCacheEnabled(Boolean baselineCacheEnabled) { this.baselineCacheEnabled = baselineCacheEnabled; }
    public Integer getMaxBaselineAgeHours() { return maxBaselineAgeHours; }
    public void setMaxBaselineAgeHours(Integer maxBaselineAgeHours) { this.maxBaselineAgeHours = maxBaselineAgeHours; }
    public AnomalyTimeBucketType getDefaultTimeBucketType() { return defaultTimeBucketType; }
    public void setDefaultTimeBucketType(AnomalyTimeBucketType defaultTimeBucketType) { this.defaultTimeBucketType = defaultTimeBucketType; }
    public Boolean getFallbackGlobalTimeBucket() { return fallbackGlobalTimeBucket; }
    public void setFallbackGlobalTimeBucket(Boolean fallbackGlobalTimeBucket) { this.fallbackGlobalTimeBucket = fallbackGlobalTimeBucket; }
}
