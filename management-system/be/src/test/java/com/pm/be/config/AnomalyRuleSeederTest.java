package com.pm.be.config;

import com.pm.be.entity.anomaly.AnomalyRuleEntity;
import com.pm.be.entity.anomaly.AnomalyStaticRuleConfigEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.repository.anomaly.AnomalyRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyRuleSeederTest {
    @Mock
    AnomalyRuleRepository anomalyRuleRepository;

    @Test
    void run_shouldSeedSignatureAttackForClientAndIpScopes() {
        when(anomalyRuleRepository.findByRuleCode(anyString())).thenReturn(Optional.empty());

        new AnomalyRuleSeeder(anomalyRuleRepository).run(null);

        List<AnomalyRuleEntity> savedRules = savedRules();
        AnomalyRuleEntity clientRule = findRule(savedRules, "SIGNATURE_ATTACK");
        AnomalyRuleEntity ipRule = findRule(savedRules, "SIGNATURE_ATTACK_IP");

        assertThat(clientRule.getScopeType()).isEqualTo(AnomalyScopeType.ENDPOINT_CLIENT);
        assertThat(clientRule.getMetric()).isEqualTo("signature_fail_count");
        assertThat(clientRule.getStaticConfig().getThresholdValue()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(ipRule.getScopeType()).isEqualTo(AnomalyScopeType.ENDPOINT_IP);
        assertThat(ipRule.getMetric()).isEqualTo("signature_fail_count");

        AnomalyRuleEntity latencyRule = findRule(savedRules, "LATENCY_SPIKE");
        assertThat(latencyRule.getMetric()).isEqualTo("slow_request_rate_5m");
        assertThat(latencyRule.getBaselineConfig().getMinAbsoluteThreshold()).isEqualByComparingTo("0.20");
    }

    @Test
    void run_existingSignatureAttack_shouldUpdateLegacyGlobalScope() {
        AnomalyRuleEntity existingRule = AnomalyRuleEntity.builder()
                .ruleCode("SIGNATURE_ATTACK")
                .scopeType(AnomalyScopeType.GLOBAL)
                .staticConfig(AnomalyStaticRuleConfigEntity.builder().build())
                .build();
        when(anomalyRuleRepository.findByRuleCode(anyString())).thenAnswer(invocation -> {
            String ruleCode = invocation.getArgument(0);
            return "SIGNATURE_ATTACK".equals(ruleCode) ? Optional.of(existingRule) : Optional.empty();
        });

        new AnomalyRuleSeeder(anomalyRuleRepository).run(null);

        AnomalyRuleEntity savedRule = findRule(savedRules(), "SIGNATURE_ATTACK");
        assertThat(savedRule).isSameAs(existingRule);
        assertThat(savedRule.getScopeType()).isEqualTo(AnomalyScopeType.ENDPOINT_CLIENT);
        assertThat(savedRule.getStaticConfig().getWindowSeconds()).isEqualTo(60);
    }

    private List<AnomalyRuleEntity> savedRules() {
        ArgumentCaptor<AnomalyRuleEntity> captor = ArgumentCaptor.forClass(AnomalyRuleEntity.class);
        org.mockito.Mockito.verify(anomalyRuleRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private AnomalyRuleEntity findRule(List<AnomalyRuleEntity> rules, String ruleCode) {
        return rules.stream()
                .filter(rule -> ruleCode.equals(rule.getRuleCode()))
                .findFirst()
                .orElseThrow();
    }
}
