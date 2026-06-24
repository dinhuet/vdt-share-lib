package com.pm.be.service.anomaly;

import com.pm.be.config.StaticRuleProperties;
import com.pm.be.dto.anomaly.*;
import com.pm.be.enums.AnomalyRuleOperator;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticRuleEngineTest {
    @Mock AnomalyRuleService anomalyRuleService;
    @Mock RedisMetricReader redisMetricReader;

    private StaticRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new StaticRuleEngine(anomalyRuleService, redisMetricReader, new StaticRuleProperties());
    }

    @Test
    void evaluate_counterBelowThreshold_shouldNotMatch() {
        SecurityLogEventMessage event = event("client-a", "10.0.0.1");
        when(anomalyRuleService.getEnabledStaticRulesByMetrics(java.util.Set.of("auth_fail_count"))).thenReturn(List.of(rule(AnomalyScopeType.ENDPOINT_CLIENT)));
        when(redisMetricReader.windowStart(event.getTimestamp(), 60)).thenReturn(1782208800L);
        when(redisMetricReader.readCounter(60, 1782208800L, AnomalyScopeType.ENDPOINT_CLIENT,
                "INBOUND_HTTP:endpoint-1:client:client-a", "auth_fail_count")).thenReturn(BigDecimal.valueOf(4));

        List<StaticRuleMatch> matches = engine.evaluate(event, extraction());

        assertThat(matches).isEmpty();
    }

    @Test
    void evaluate_counterEqualThresholdWithGte_shouldMatch() {
        SecurityLogEventMessage event = event("client-a", "10.0.0.1");
        when(anomalyRuleService.getEnabledStaticRulesByMetrics(java.util.Set.of("auth_fail_count"))).thenReturn(List.of(rule(AnomalyScopeType.ENDPOINT_CLIENT)));
        when(redisMetricReader.windowStart(event.getTimestamp(), 60)).thenReturn(1782208800L);
        when(redisMetricReader.readCounter(60, 1782208800L, AnomalyScopeType.ENDPOINT_CLIENT,
                "INBOUND_HTTP:endpoint-1:client:client-a", "auth_fail_count")).thenReturn(BigDecimal.valueOf(5));

        List<StaticRuleMatch> matches = engine.evaluate(event, extraction());

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).currentValue()).isEqualByComparingTo("5");
    }

    @Test
    void evaluate_missingRedisKeyAsZero_shouldNotMatch() {
        SecurityLogEventMessage event = event("client-a", "10.0.0.1");
        when(anomalyRuleService.getEnabledStaticRulesByMetrics(java.util.Set.of("auth_fail_count"))).thenReturn(List.of(rule(AnomalyScopeType.ENDPOINT_CLIENT)));
        when(redisMetricReader.windowStart(event.getTimestamp(), 60)).thenReturn(1782208800L);
        when(redisMetricReader.readCounter(60, 1782208800L, AnomalyScopeType.ENDPOINT_CLIENT,
                "INBOUND_HTTP:endpoint-1:client:client-a", "auth_fail_count")).thenReturn(BigDecimal.ZERO);

        assertThat(engine.evaluate(event, extraction())).isEmpty();
    }

    @Test
    void evaluate_missingClientForClientScopedRule_shouldSkip() {
        SecurityLogEventMessage event = event(null, "10.0.0.1");
        when(anomalyRuleService.getEnabledStaticRulesByMetrics(java.util.Set.of("auth_fail_count"))).thenReturn(List.of(rule(AnomalyScopeType.ENDPOINT_CLIENT)));

        assertThat(engine.evaluate(event, extraction())).isEmpty();
    }

    private StaticRuleDefinition rule(AnomalyScopeType scopeType) {
        return new StaticRuleDefinition(UUID.randomUUID(), "AUTH_BRUTE_FORCE", "auth_fail_count", AnomalySeverity.HIGH,
                scopeType, null, BigDecimal.valueOf(5), 60, 1, 1, AnomalyRuleOperator.GTE);
    }

    private MetricExtractionResult extraction() {
        return new MetricExtractionResult(List.of(new MetricIncrement("auth_fail_count", 1, 60, AnomalyScopeType.ENDPOINT_CLIENT,
                "INBOUND_HTTP:endpoint-1:client:client-a", Instant.parse("2026-06-23T10:00:01Z"), 120)), List.of());
    }

    private SecurityLogEventMessage event(String clientId, String sourceIp) {
        SecurityLogEventMessage event = new SecurityLogEventMessage();
        event.setTimestamp(Instant.parse("2026-06-23T10:00:01Z"));
        event.setFlowType("INBOUND_HTTP");
        event.setEndpointId("endpoint-1");
        event.setEndpointName("create-order");
        event.setServiceName("order-service");
        event.setClientId(clientId);
        event.setSourceIp(sourceIp);
        event.setResultCode("AUTH_API_KEY_INVALID");
        return event;
    }
}
