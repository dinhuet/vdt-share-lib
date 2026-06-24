package com.pm.be.dto.request.anomaly;

import com.pm.be.enums.AnomalyRuleOperator;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnomalyStaticRuleConfigRequest {
    BigDecimal thresholdValue;
    Integer windowSeconds;
    Integer minSampleCount;
    Integer consecutiveWindows;
    AnomalyRuleOperator operator;
}
