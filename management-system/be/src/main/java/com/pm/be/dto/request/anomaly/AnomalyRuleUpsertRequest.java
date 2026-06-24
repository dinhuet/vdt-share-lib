package com.pm.be.dto.request.anomaly;

import com.pm.be.enums.AnomalyRuleType;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnomalyRuleUpsertRequest {
    String ruleCode;
    String name;
    String description;
    AnomalyRuleType ruleType;
    String metric;
    AnomalySeverity severity;
    AnomalyScopeType scopeType;
    String scopeId;
    Boolean enabled;
    UUID notificationRuleId;
    Integer cooldownMinutes;
    AnomalyStaticRuleConfigRequest staticConfig;
    AnomalyBaselineRuleConfigRequest baselineConfig;
}
