package com.pm.be.service.anomaly;

import com.pm.be.config.BaselineRuleProperties;
import com.pm.be.dto.anomaly.*;
import com.pm.be.entity.anomaly.AnomalyBaselineEntity;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaselineRuleEngine {
    private static final Set<String> SUPPORTED_METRICS = Set.of(
            "request_count_1m", "auth_fail_count_5m", "rate_limit_exceeded_count_5m", "request_too_large_count_5m",
            "error_rate_5m", "denied_rate_5m", "timeout_rate_5m", "retry_rate_5m", "auth_fail_rate_5m", "slow_request_rate_5m"
    );

    private final BaselineRuleConfigService ruleConfigService;
    private final CurrentMetricReader currentMetricReader;
    private final BaselineLookupService baselineLookupService;
    private final BaselineRuleProperties properties;

    public List<BaselineRuleMatch> evaluate(SecurityLogEventMessage event, MetricExtractionResult extractionResult) {
        if (!Boolean.TRUE.equals(properties.getEnabled()) || event == null || event.getTimestamp() == null) {
            return List.of();
        }
        try {
            Map<AnomalyScopeType, ScopeCandidate> candidates = scopeCandidates(event);
            List<BaselineRuleMatch> matches = new ArrayList<>();
            for (BaselineRuleDefinition rule : ruleConfigService.loadEnabledBaselineRules()) {
                evaluateRule(event, rule, candidates).ifPresent(matches::add);
            }
            return List.copyOf(matches);
        } catch (RuntimeException e) {
            log.warn("Skip baseline rule evaluation because rule evaluation failed: endpointId={}", event.getEndpointId(), e);
            return List.of();
        }
    }

    private Optional<BaselineRuleMatch> evaluateRule(SecurityLogEventMessage event, BaselineRuleDefinition rule,
                                                     Map<AnomalyScopeType, ScopeCandidate> candidates) {
        if (!isValid(rule) || !SUPPORTED_METRICS.contains(rule.metric())) {
            return Optional.empty();
        }
        ScopeCandidate candidate = resolveScope(rule, candidates);
        if (candidate == null) {
            return Optional.empty();
        }
        long currentWindowStart = currentMetricReader.windowStart(event.getTimestamp(), rule.windowSeconds());
        String aggregation = "P" + rule.percentile();
        ResolvedBaseline resolvedBaseline = loadBaseline(event, rule, candidate, aggregation).orElse(null);
        if (resolvedBaseline == null || !trusted(resolvedBaseline.baseline(), rule)) {
            return Optional.empty();
        }
        BigDecimal threshold = threshold(resolvedBaseline.baseline(), rule);
        BigDecimal lastValue = null;
        for (int i = 0; i < rule.consecutiveWindows(); i++) {
            long windowStart = currentWindowStart - ((long) i * rule.windowSeconds());
            Optional<CurrentMetricValue> current = currentMetricReader.readAtWindow(rule.metric(), rule.windowSeconds(), windowStart,
                    candidate.scopeType(), candidate.scopeKey(), rule.minSampleCount());
            if (current.isEmpty() || current.get().value().compareTo(threshold) <= 0) {
                return Optional.empty();
            }
            lastValue = current.get().value();
        }
        Instant windowStart = Instant.ofEpochSecond(currentWindowStart);
        return Optional.of(new BaselineRuleMatch(rule.ruleId(), rule.ruleCode(), rule.severity(), rule.metric(), candidate.scopeType(),
                candidate.scopeKey(), candidate.identity(), lastValue, resolvedBaseline.baseline().getValue(), threshold, rule.multiplier(),
                rule.minAbsoluteThreshold(), rule.windowSeconds(), resolvedBaseline.timeBucket(), rule.notificationRuleId(), rule.cooldownMinutes(), windowStart,
                windowStart.plusSeconds(rule.windowSeconds()), event));
    }

    private Optional<ResolvedBaseline> loadBaseline(SecurityLogEventMessage event, BaselineRuleDefinition rule, ScopeCandidate candidate, String aggregation) {
        AnomalyTimeBucketType configuredType = rule.timeBucketType() == null ? properties.getDefaultTimeBucketType() : rule.timeBucketType();
        String timeBucket = timeBucket(configuredType, event.getTimestamp());
        Optional<AnomalyBaselineEntity> baseline = baselineLookupService.lookup(rule.metric(), candidate.scopeType(), candidate.scopeKey(),
                configuredType, timeBucket, rule.historyDays(), aggregation, rule.windowSeconds());
        if (baseline.isPresent()) {
            return Optional.of(new ResolvedBaseline(baseline.get(), timeBucket));
        }
        if (configuredType != AnomalyTimeBucketType.GLOBAL && Boolean.TRUE.equals(properties.getFallbackGlobalTimeBucket())) {
            Optional<AnomalyBaselineEntity> global = baselineLookupService.lookup(rule.metric(), candidate.scopeType(), candidate.scopeKey(),
                    AnomalyTimeBucketType.GLOBAL, "GLOBAL", rule.historyDays(), aggregation, rule.windowSeconds());
            return global.map(entity -> new ResolvedBaseline(entity, "GLOBAL"));
        }
        log.debug("No trusted baseline found ruleCode={} metric={} scopeType={} scopeKey={}",
                rule.ruleCode(), rule.metric(), candidate.scopeType(), candidate.scopeKey());
        return Optional.empty();
    }

    private boolean trusted(AnomalyBaselineEntity baseline, BaselineRuleDefinition rule) {
        if (baseline.getValue() == null || baseline.getSampleCount() == null || baseline.getSampleCount() < rule.minSampleCount()) {
            return false;
        }
        if (baseline.getCalculatedAt() == null) {
            return false;
        }
        int maxAgeHours = properties.getMaxBaselineAgeHours() == null || properties.getMaxBaselineAgeHours() <= 0 ? 2 : properties.getMaxBaselineAgeHours();
        return !baseline.getCalculatedAt().isBefore(LocalDateTime.now().minusHours(maxAgeHours));
    }

    private BigDecimal threshold(AnomalyBaselineEntity baseline, BaselineRuleDefinition rule) {
        BigDecimal threshold = baseline.getValue().multiply(rule.multiplier()).max(rule.minAbsoluteThreshold());
        return rule.maxAbsoluteThreshold() == null ? threshold : threshold.min(rule.maxAbsoluteThreshold());
    }

    private ScopeCandidate resolveScope(BaselineRuleDefinition rule, Map<AnomalyScopeType, ScopeCandidate> candidates) {
        if (rule.configuredScopeType() == AnomalyScopeType.ENDPOINT || rule.configuredScopeType() == AnomalyScopeType.ENDPOINT_CLIENT
                || rule.configuredScopeType() == AnomalyScopeType.SERVICE) {
            return candidates.get(rule.configuredScopeType());
        }
        if ("auth_fail_rate_5m".equals(rule.metric())) {
            return candidates.getOrDefault(AnomalyScopeType.ENDPOINT_CLIENT, candidates.get(AnomalyScopeType.ENDPOINT));
        }
        return candidates.getOrDefault(AnomalyScopeType.ENDPOINT, candidates.get(AnomalyScopeType.ENDPOINT_CLIENT));
    }

    private boolean isValid(BaselineRuleDefinition rule) {
        return rule != null && StringUtils.hasText(rule.ruleCode()) && StringUtils.hasText(rule.metric()) && rule.severity() != null
                && rule.windowSeconds() > 0 && rule.minSampleCount() > 0 && rule.consecutiveWindows() > 0
                && rule.multiplier() != null && rule.minAbsoluteThreshold() != null;
    }

    private String timeBucket(AnomalyTimeBucketType type, Instant timestamp) {
        if (type == AnomalyTimeBucketType.GLOBAL) {
            return "GLOBAL";
        }
        int hour = timestamp.atZone(ZoneId.systemDefault()).getHour();
        return "HOUR_%02d".formatted(hour);
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
        }
        String serviceName = trim(event.getServiceName());
        if (StringUtils.hasText(serviceName)) {
            candidates.put(AnomalyScopeType.SERVICE, new ScopeCandidate(AnomalyScopeType.SERVICE, flowType + ":service:" + serviceName, serviceName));
        }
        return candidates;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record ResolvedBaseline(AnomalyBaselineEntity baseline, String timeBucket) {
    }
}
