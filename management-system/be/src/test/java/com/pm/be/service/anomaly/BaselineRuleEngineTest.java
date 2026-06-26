package com.pm.be.service.anomaly;

import com.pm.be.config.BaselineRuleProperties;
import com.pm.be.dto.anomaly.*;
import com.pm.be.entity.anomaly.AnomalyBaselineEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalySeverity;
import com.pm.be.enums.AnomalyTimeBucketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaselineRuleEngineTest {
    @Mock BaselineRuleConfigService ruleConfigService;
    @Mock CurrentMetricReader currentMetricReader;
    @Mock BaselineLookupService baselineLookupService;

    private BaselineRuleEngine engine;

    @BeforeEach
    void setUp() {
        BaselineRuleProperties properties = new BaselineRuleProperties();
        properties.setMaxBaselineAgeHours(2);
        engine = new BaselineRuleEngine(ruleConfigService, currentMetricReader, baselineLookupService, properties);
    }

    @Test
    void evaluate_currentAboveThreshold_shouldMatch() {
        SecurityLogEventMessage event = event();
        BaselineRuleDefinition rule = rule("TRAFFIC_SPIKE", "request_count_1m", AnomalyScopeType.ENDPOINT, 50, 1);
        when(ruleConfigService.loadEnabledBaselineRules()).thenReturn(List.of(rule));
        when(currentMetricReader.windowStart(event.getTimestamp(), 60)).thenReturn(1782208800L);
        when(baselineLookupService.lookup(eq("request_count_1m"), eq(AnomalyScopeType.ENDPOINT), eq("INBOUND_HTTP:endpoint-1"),
                eq(AnomalyTimeBucketType.SAME_HOUR), anyString(), eq(7), eq("P95"), eq(60))).thenReturn(Optional.of(baseline(80, 100)));
        when(currentMetricReader.readAtWindow("request_count_1m", 60, 1782208800L, AnomalyScopeType.ENDPOINT,
                "INBOUND_HTTP:endpoint-1", 50)).thenReturn(Optional.of(current("request_count_1m", 170)));

        List<BaselineRuleMatch> matches = engine.evaluate(event, MetricExtractionResult.empty());

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).thresholdValue()).isEqualByComparingTo("160");
    }

    @Test
    void evaluate_currentBelowThreshold_shouldNotMatch() {
        SecurityLogEventMessage event = event();
        BaselineRuleDefinition rule = rule("TRAFFIC_SPIKE", "request_count_1m", AnomalyScopeType.ENDPOINT, 50, 1);
        when(ruleConfigService.loadEnabledBaselineRules()).thenReturn(List.of(rule));
        when(currentMetricReader.windowStart(event.getTimestamp(), 60)).thenReturn(1782208800L);
        when(baselineLookupService.lookup(anyString(), any(), anyString(), any(), anyString(), anyInt(), anyString(), anyInt()))
                .thenReturn(Optional.of(baseline(80, 100)));
        when(currentMetricReader.readAtWindow(anyString(), anyInt(), anyLong(), any(), anyString(), anyInt()))
                .thenReturn(Optional.of(current("request_count_1m", 159)));

        assertThat(engine.evaluate(event, MetricExtractionResult.empty())).isEmpty();
    }

    @Test
    void evaluate_missingBaseline_shouldSkip() {
        when(ruleConfigService.loadEnabledBaselineRules()).thenReturn(List.of(rule("TRAFFIC_SPIKE", "request_count_1m", AnomalyScopeType.ENDPOINT, 50, 1)));
        when(currentMetricReader.windowStart(any(), eq(60))).thenReturn(1782208800L);
        when(baselineLookupService.lookup(anyString(), any(), anyString(), any(), anyString(), anyInt(), anyString(), anyInt()))
                .thenReturn(Optional.empty());

        assertThat(engine.evaluate(event(), MetricExtractionResult.empty())).isEmpty();
    }

    @Test
    void evaluate_insufficientBaselineSample_shouldSkip() {
        when(ruleConfigService.loadEnabledBaselineRules()).thenReturn(List.of(rule("TRAFFIC_SPIKE", "request_count_1m", AnomalyScopeType.ENDPOINT, 50, 1)));
        when(currentMetricReader.windowStart(any(), eq(60))).thenReturn(1782208800L);
        when(baselineLookupService.lookup(anyString(), any(), anyString(), any(), anyString(), anyInt(), anyString(), anyInt()))
                .thenReturn(Optional.of(baseline(80, 10)));

        assertThat(engine.evaluate(event(), MetricExtractionResult.empty())).isEmpty();
    }

    @Test
    void evaluate_sameHourMissing_shouldFallbackGlobal() {
        SecurityLogEventMessage event = event();
        BaselineRuleDefinition rule = rule("TRAFFIC_SPIKE", "request_count_1m", AnomalyScopeType.ENDPOINT, 50, 1);
        when(ruleConfigService.loadEnabledBaselineRules()).thenReturn(List.of(rule));
        when(currentMetricReader.windowStart(event.getTimestamp(), 60)).thenReturn(1782208800L);
        when(baselineLookupService.lookup(eq("request_count_1m"), eq(AnomalyScopeType.ENDPOINT), eq("INBOUND_HTTP:endpoint-1"),
                eq(AnomalyTimeBucketType.SAME_HOUR), anyString(), eq(7), eq("P95"), eq(60))).thenReturn(Optional.empty());
        when(baselineLookupService.lookup(eq("request_count_1m"), eq(AnomalyScopeType.ENDPOINT), eq("INBOUND_HTTP:endpoint-1"),
                eq(AnomalyTimeBucketType.GLOBAL), eq("GLOBAL"), eq(7), eq("P95"), eq(60))).thenReturn(Optional.of(baseline(80, 100)));
        when(currentMetricReader.readAtWindow(anyString(), anyInt(), anyLong(), any(), anyString(), anyInt()))
                .thenReturn(Optional.of(current("request_count_1m", 170)));

        assertThat(engine.evaluate(event, MetricExtractionResult.empty())).hasSize(1);
    }

    private BaselineRuleDefinition rule(String code, String metric, AnomalyScopeType scopeType, int minSampleCount, int consecutiveWindows) {
        return new BaselineRuleDefinition(UUID.randomUUID(), code, metric, AnomalySeverity.MEDIUM, scopeType, null, null, null,
                7, AnomalyTimeBucketType.SAME_HOUR, 95, BigDecimal.valueOf(2), BigDecimal.valueOf(50), null,
                minSampleCount, consecutiveWindows, metric.endsWith("_1m") ? 60 : 300);
    }

    private AnomalyBaselineEntity baseline(int value, long sampleCount) {
        AnomalyBaselineEntity entity = new AnomalyBaselineEntity();
        entity.setValue(BigDecimal.valueOf(value));
        entity.setSampleCount(sampleCount);
        entity.setCalculatedAt(LocalDateTime.now());
        return entity;
    }

    private CurrentMetricValue current(String metric, int value) {
        return new CurrentMetricValue(metric, BigDecimal.valueOf(value), BigDecimal.valueOf(value), null, false);
    }

    private SecurityLogEventMessage event() {
        SecurityLogEventMessage event = new SecurityLogEventMessage();
        event.setTimestamp(Instant.parse("2026-06-23T10:00:01Z"));
        event.setFlowType("INBOUND_HTTP");
        event.setEndpointId("endpoint-1");
        event.setServiceName("order-service");
        return event;
    }
}
