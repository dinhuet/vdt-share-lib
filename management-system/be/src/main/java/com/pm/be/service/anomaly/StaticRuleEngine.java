package com.pm.be.service.anomaly;

import com.pm.be.config.StaticRuleProperties;
import com.pm.be.dto.anomaly.*;
import com.pm.be.enums.AnomalyRuleOperator;
import com.pm.be.enums.AnomalyScopeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaticRuleEngine {
    private final AnomalyRuleService anomalyRuleService;
    private final RedisMetricReader redisMetricReader;
    private final StaticRuleProperties properties;

    public List<StaticRuleMatch> evaluate(SecurityLogEventMessage event, MetricExtractionResult extractionResult) {
        if (!Boolean.TRUE.equals(properties.getEnabled()) || event == null || event.getTimestamp() == null || extractionResult == null) {
            return List.of();
        }
        Set<String> metrics = extractionResult.metricIncrements() == null
                ? Set.of()
                : extractionResult.metricIncrements().stream()
                .map(MetricIncrement::metric)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (metrics.isEmpty()) {
            return List.of();
        }

        try {
            Map<AnomalyScopeType, ScopeCandidate> candidates = scopeCandidates(event);
            List<StaticRuleMatch> matches = new ArrayList<>();
            for (StaticRuleDefinition rule : anomalyRuleService.getEnabledStaticRulesByMetrics(metrics)) {
                evaluateRule(event, rule, candidates).ifPresent(matches::add);
            }
            return List.copyOf(matches);
        } catch (RuntimeException e) {
            log.warn("Skip static rule evaluation because Redis/rule evaluation failed: endpointId={} resultCode={}",
                    event.getEndpointId(), event.getResultCode(), e);
            return List.of();
        }
    }

    private Optional<StaticRuleMatch> evaluateRule(SecurityLogEventMessage event, StaticRuleDefinition rule, Map<AnomalyScopeType, ScopeCandidate> candidates) {
        if (!isValid(rule)) {
            log.warn("Skip invalid static anomaly rule: ruleId={} ruleCode={}", rule == null ? null : rule.ruleId(), rule == null ? null : rule.ruleCode());
            return Optional.empty();
        }
        ScopeCandidate candidate = candidates.get(rule.scopeType());
        if (candidate == null) {
            return Optional.empty();
        }
        long currentWindowStart = redisMetricReader.windowStart(event.getTimestamp(), rule.windowSeconds());
        BigDecimal lastValue = BigDecimal.ZERO;
        int consecutiveWindows = rule.consecutiveWindows() == null ? 1 : rule.consecutiveWindows();
        for (int i = 0; i < consecutiveWindows; i++) {
            long windowStart = currentWindowStart - ((long) i * rule.windowSeconds());
            BigDecimal value = redisMetricReader.readCounter(rule.windowSeconds(), windowStart, rule.scopeType(), candidate.scopeKey(), rule.metric());
            lastValue = value;
            if (!matches(value, rule.thresholdValue(), rule.operator())) {
                return Optional.empty();
            }
        }
        Instant windowStart = Instant.ofEpochSecond(currentWindowStart);
        return Optional.of(new StaticRuleMatch(
                rule.ruleId(),
                rule.ruleCode(),
                rule.severity(),
                rule.metric(),
                rule.scopeType(),
                candidate.scopeKey(),
                candidate.identity(),
                lastValue,
                rule.thresholdValue(),
                rule.windowSeconds(),
                windowStart,
                windowStart.plusSeconds(rule.windowSeconds()),
                rule.notificationRuleId(),
                rule.cooldownMinutes(),
                event));
    }

    private boolean isValid(StaticRuleDefinition rule) {
        return rule != null
                && StringUtils.hasText(rule.ruleCode())
                && StringUtils.hasText(rule.metric())
                && rule.severity() != null
                && rule.scopeType() != null
                && rule.thresholdValue() != null
                && rule.thresholdValue().compareTo(BigDecimal.ZERO) > 0
                && rule.windowSeconds() != null
                && rule.windowSeconds() > 0
                && (rule.consecutiveWindows() == null || rule.consecutiveWindows() > 0);
    }

    private boolean matches(BigDecimal currentValue, BigDecimal thresholdValue, AnomalyRuleOperator operator) {
        AnomalyRuleOperator effectiveOperator = operator == null ? AnomalyRuleOperator.GTE : operator;
        int comparison = currentValue.compareTo(thresholdValue);
        return switch (effectiveOperator) {
            case GT -> comparison > 0;
            case GTE -> comparison >= 0;
            case LT -> comparison < 0;
            case LTE -> comparison <= 0;
        };
    }

    private Map<AnomalyScopeType, ScopeCandidate> scopeCandidates(SecurityLogEventMessage event) {
        Map<AnomalyScopeType, ScopeCandidate> candidates = new EnumMap<>(AnomalyScopeType.class);
        String flowType = StringUtils.hasText(event.getFlowType()) ? event.getFlowType().trim().toUpperCase(Locale.ROOT) : "UNKNOWN";
        String endpointId = trim(event.getEndpointId());
        if (StringUtils.hasText(endpointId)) {
            candidates.put(AnomalyScopeType.ENDPOINT, new ScopeCandidate(AnomalyScopeType.ENDPOINT, flowType + ":" + endpointId, endpointId));
            String clientId = trim(event.getClientId());
            if (StringUtils.hasText(clientId)) {
                candidates.put(AnomalyScopeType.ENDPOINT_CLIENT,
                        new ScopeCandidate(AnomalyScopeType.ENDPOINT_CLIENT, flowType + ":" + endpointId + ":client:" + clientId, clientId));
            }
            String sourceIp = trim(event.getSourceIp());
            if (StringUtils.hasText(sourceIp)) {
                candidates.put(AnomalyScopeType.ENDPOINT_IP,
                        new ScopeCandidate(AnomalyScopeType.ENDPOINT_IP, flowType + ":" + endpointId + ":ip:" + sourceIp, sourceIp));
            }
        }
        String serviceName = trim(event.getServiceName());
        if (StringUtils.hasText(serviceName)) {
            candidates.put(AnomalyScopeType.SERVICE, new ScopeCandidate(AnomalyScopeType.SERVICE, flowType + ":service:" + serviceName, serviceName));
        }
        candidates.put(AnomalyScopeType.GLOBAL, new ScopeCandidate(AnomalyScopeType.GLOBAL, flowType + ":global", "global"));
        return candidates;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
