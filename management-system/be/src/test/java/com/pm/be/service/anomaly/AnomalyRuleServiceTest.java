package com.pm.be.service.anomaly;

import com.pm.be.config.StaticRuleProperties;
import com.pm.be.dto.request.anomaly.AnomalyBaselineRuleConfigRequest;
import com.pm.be.dto.request.anomaly.AnomalyRuleUpsertRequest;
import com.pm.be.entity.anomaly.AnomalyBaselineRuleConfigEntity;
import com.pm.be.entity.anomaly.AnomalyRuleEntity;
import com.pm.be.enums.AnomalyRuleType;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.AnomalyTimeBucketType;
import com.pm.be.repository.anomaly.AnomalyRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyRuleServiceTest {
    @Mock
    AnomalyRuleRepository anomalyRuleRepository;

    AnomalyRuleService service;

    @BeforeEach
    void setUp() {
        service = new AnomalyRuleService(anomalyRuleRepository, new StaticRuleProperties());
    }

    @Test
    void update_existingBaselineRule_shouldReuseManagedBaselineConfig() {
        UUID ruleId = UUID.randomUUID();
        AnomalyRuleEntity entity = AnomalyRuleEntity.builder()
                .id(ruleId)
                .ruleCode("TRAFFIC_SPIKE")
                .name("Traffic spike")
                .ruleType(AnomalyRuleType.BASELINE)
                .metric("request_count_1m")
                .severity(AnomalySeverity.MEDIUM)
                .scopeType(AnomalyScopeType.ENDPOINT)
                .scopeId("endpoint-1")
                .enabled(true)
                .build();
        AnomalyBaselineRuleConfigEntity existingConfig = AnomalyBaselineRuleConfigEntity.builder()
                .rule(entity)
                .historyDays(7)
                .timeBucketType(AnomalyTimeBucketType.SAME_HOUR)
                .percentile(BigDecimal.valueOf(95))
                .multiplier(BigDecimal.valueOf(2))
                .minAbsoluteThreshold(BigDecimal.valueOf(50))
                .minSampleCount(50)
                .consecutiveWindows(2)
                .windowSeconds(60)
                .build();
        entity.setBaselineConfig(existingConfig);
        AnomalyRuleUpsertRequest request = baselineRequest("TRAFFIC_SPIKE", BigDecimal.ONE, BigDecimal.ZERO, 1, 1);

        when(anomalyRuleRepository.findById(ruleId)).thenReturn(Optional.of(entity));
        when(anomalyRuleRepository.findByRuleCode("TRAFFIC_SPIKE")).thenReturn(Optional.of(entity));
        when(anomalyRuleRepository.save(any(AnomalyRuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(ruleId, request);

        assertThat(entity.getBaselineConfig()).isSameAs(existingConfig);
        assertThat(entity.getBaselineConfig().getMultiplier()).isEqualByComparingTo("1");
        assertThat(entity.getBaselineConfig().getMinAbsoluteThreshold()).isEqualByComparingTo("0");
        assertThat(entity.getBaselineConfig().getMinSampleCount()).isEqualTo(1);
        assertThat(entity.getBaselineConfig().getConsecutiveWindows()).isEqualTo(1);
    }

    private AnomalyRuleUpsertRequest baselineRequest(String ruleCode, BigDecimal multiplier,
                                                     BigDecimal minAbsoluteThreshold, int minSampleCount,
                                                     int consecutiveWindows) {
        return AnomalyRuleUpsertRequest.builder()
                .ruleCode(ruleCode)
                .name("Traffic spike")
                .ruleType(AnomalyRuleType.BASELINE)
                .metric("request_count_1m")
                .severity(AnomalySeverity.MEDIUM)
                .scopeType(AnomalyScopeType.ENDPOINT)
                .scopeId("endpoint-1")
                .enabled(true)
                .cooldownMinutes(5)
                .baselineConfig(AnomalyBaselineRuleConfigRequest.builder()
                        .historyDays(7)
                        .timeBucketType(AnomalyTimeBucketType.SAME_HOUR)
                        .percentile(BigDecimal.valueOf(95))
                        .multiplier(multiplier)
                        .minAbsoluteThreshold(minAbsoluteThreshold)
                        .minSampleCount(minSampleCount)
                        .consecutiveWindows(consecutiveWindows)
                        .windowSeconds(60)
                        .build())
                .build();
    }
}
