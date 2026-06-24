package com.pm.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vdt.anomaly.baseline-job")
public class BaselineJobProperties {
    private Boolean enabled = true;
    private String cron = "0 5 * * * *";
    private Integer defaultHistoryDays = 7;
    private Boolean cacheEnabled = true;
    private Integer cacheTtlSeconds = 7200;
    private String elasticsearchUrl = "http://localhost:9200";
    private String indexPattern = "security-logs-*";
    private String timestampField = "timestamp";
    private Integer maxScopes = 500;
    private Integer requestTimeoutSeconds = 30;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public Integer getDefaultHistoryDays() { return defaultHistoryDays; }
    public void setDefaultHistoryDays(Integer defaultHistoryDays) { this.defaultHistoryDays = defaultHistoryDays; }
    public Boolean getCacheEnabled() { return cacheEnabled; }
    public void setCacheEnabled(Boolean cacheEnabled) { this.cacheEnabled = cacheEnabled; }
    public Integer getCacheTtlSeconds() { return cacheTtlSeconds; }
    public void setCacheTtlSeconds(Integer cacheTtlSeconds) { this.cacheTtlSeconds = cacheTtlSeconds; }
    public String getElasticsearchUrl() { return elasticsearchUrl; }
    public void setElasticsearchUrl(String elasticsearchUrl) { this.elasticsearchUrl = elasticsearchUrl; }
    public String getIndexPattern() { return indexPattern; }
    public void setIndexPattern(String indexPattern) { this.indexPattern = indexPattern; }
    public String getTimestampField() { return timestampField; }
    public void setTimestampField(String timestampField) { this.timestampField = timestampField; }
    public Integer getMaxScopes() { return maxScopes; }
    public void setMaxScopes(Integer maxScopes) { this.maxScopes = maxScopes; }
    public Integer getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    public void setRequestTimeoutSeconds(Integer requestTimeoutSeconds) { this.requestTimeoutSeconds = requestTimeoutSeconds; }
}
