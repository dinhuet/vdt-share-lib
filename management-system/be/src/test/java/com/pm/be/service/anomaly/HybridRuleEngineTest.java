package com.pm.be.service.anomaly;

import com.pm.be.config.HybridRuleProperties;
import com.pm.be.dto.anomaly.*;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HybridRuleEngineTest {
    private HybridRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new HybridRuleEngine(new HybridRuleProperties());
    }

    @Test
    void evaluate_staticOnly_shouldNotCreateHybrid() {
        assertThat(engine.evaluate(List.of(staticMatch("AUTH_BRUTE_FORCE", "client-a")), List.of())).isEmpty();
    }

    @Test
    void evaluate_baselineOnly_shouldNotCreateHybrid() {
        assertThat(engine.evaluate(List.of(), List.of(baselineMatch("AUTH_FAIL_RATE_SPIKE", "client-a")))).isEmpty();
    }

    @Test
    void evaluate_staticAndBaseline_shouldUpgradeSeverity() {
        List<AnomalyRuleMatch> matches = engine.evaluate(
                List.of(staticMatch("AUTH_BRUTE_FORCE", "client-a")),
                List.of(baselineMatch("AUTH_FAIL_RATE_SPIKE", "client-a")));

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).ruleCode()).isEqualTo("AUTH_BRUTE_FORCE_HYBRID");
        assertThat(matches.get(0).severity()).isEqualTo(AnomalySeverity.CRITICAL);
    }

    @Test
    void evaluate_unrelatedScopes_shouldNotCombine() {
        assertThat(engine.evaluate(List.of(staticMatch("AUTH_BRUTE_FORCE", "client-a")),
                List.of(baselineMatch("AUTH_FAIL_RATE_SPIKE", "client-b")))).isEmpty();
    }

    private StaticRuleMatch staticMatch(String ruleCode, String clientId) {
        SecurityLogEventMessage event = event(clientId);
        return new StaticRuleMatch(UUID.randomUUID(), ruleCode, AnomalySeverity.HIGH, "auth_fail_count", AnomalyScopeType.ENDPOINT_CLIENT,
                "INBOUND_HTTP:endpoint-1:client:" + clientId, clientId, BigDecimal.valueOf(5), BigDecimal.valueOf(5), 60,
                Instant.parse("2026-06-23T10:00:00Z"), Instant.parse("2026-06-23T10:01:00Z"), null, null, event);
    }

    private BaselineRuleMatch baselineMatch(String ruleCode, String clientId) {
        SecurityLogEventMessage event = event(clientId);
        return new BaselineRuleMatch(UUID.randomUUID(), ruleCode, AnomalySeverity.HIGH, "auth_fail_rate_5m", AnomalyScopeType.ENDPOINT_CLIENT,
                "INBOUND_HTTP:endpoint-1:client:" + clientId, clientId, BigDecimal.valueOf(0.4), BigDecimal.valueOf(0.05), BigDecimal.valueOf(0.3),
                BigDecimal.valueOf(2), BigDecimal.valueOf(0.2), 300, "HOUR_10", null, null,
                Instant.parse("2026-06-23T10:00:00Z"), Instant.parse("2026-06-23T10:05:00Z"), event);
    }

    private SecurityLogEventMessage event(String clientId) {
        SecurityLogEventMessage event = new SecurityLogEventMessage();
        event.setTimestamp(Instant.parse("2026-06-23T10:00:01Z"));
        event.setFlowType("INBOUND_HTTP");
        event.setEndpointId("endpoint-1");
        event.setClientId(clientId);
        return event;
    }
}
