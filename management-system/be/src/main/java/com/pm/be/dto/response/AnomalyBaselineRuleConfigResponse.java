package com.pm.be.dto.response;

import com.pm.be.enums.AnomalyTimeBucketType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnomalyBaselineRuleConfigResponse {
    Integer historyDays;
    AnomalyTimeBucketType timeBucketType;
    BigDecimal percentile;
    BigDecimal multiplier;
    BigDecimal minAbsoluteThreshold;
    BigDecimal maxAbsoluteThreshold;
    Integer minSampleCount;
    Integer consecutiveWindows;
}
