package com.pm.be.dto.response.anomaly;

import com.pm.be.enums.AnomalyScopeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SecurityAlertOccurrenceResponse {
    private UUID id;
    private UUID alertId;
    private String ruleCode;
    private String ruleType;
    private String metric;
    private AnomalyScopeType scopeType;
    private String scopeKey;
    private BigDecimal currentValue;
    private BigDecimal thresholdValue;
    private BigDecimal baselineValue;
    private Integer windowSeconds;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private String timeBucket;
    private LocalDateTime eventTimestamp;
    private LocalDateTime createdAt;
}
