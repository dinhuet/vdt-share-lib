package com.pm.be.service.anomaly;

import com.pm.be.config.HybridRuleProperties;
import com.pm.be.dto.anomaly.*;
import com.pm.be.enums.AnomalySeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HybridRuleEngine {
    private final HybridRuleProperties properties;

    public List<AnomalyRuleMatch> evaluate(List<StaticRuleMatch> staticMatches, List<BaselineRuleMatch> baselineMatches) {
        if (!Boolean.TRUE.equals(properties.getEnabled()) || staticMatches == null || baselineMatches == null) {
            return List.of();
        }
        List<AnomalyRuleMatch> matches = new ArrayList<>();
        addHybrid(matches, staticMatches, baselineMatches, "AUTH_BRUTE_FORCE", "AUTH_FAIL_RATE_SPIKE", "AUTH_BRUTE_FORCE_HYBRID");
        addHybrid(matches, staticMatches, baselineMatches, "RATE_LIMIT_ABUSE", "TRAFFIC_SPIKE", "RATE_LIMIT_ABUSE_HYBRID");
        addHybrid(matches, staticMatches, baselineMatches, "RATE_LIMIT_ABUSE", "rate_limit_exceeded_count_5m", "RATE_LIMIT_ABUSE_HYBRID");
        addHybrid(matches, staticMatches, baselineMatches, "PAYLOAD_SIZE_ABUSE", "request_too_large_count_5m", "PAYLOAD_SIZE_ABUSE_HYBRID");
        addHybrid(matches, staticMatches, baselineMatches, "SLOW_REQUEST_BURST", "TIMEOUT_RATE_SPIKE", "TIMEOUT_SPIKE_HYBRID");
        return List.copyOf(matches);
    }

    private void addHybrid(List<AnomalyRuleMatch> matches, List<StaticRuleMatch> staticMatches, List<BaselineRuleMatch> baselineMatches,
                           String staticRuleCode, String baselineRuleOrMetric, String hybridRuleCode) {
        for (StaticRuleMatch staticMatch : staticMatches) {
            if (!staticRuleCode.equals(staticMatch.ruleCode())) {
                continue;
            }
            Optional<BaselineRuleMatch> baseline = baselineMatches.stream()
                    .filter(match -> related(match, baselineRuleOrMetric))
                    .filter(match -> sameScope(staticMatch, match))
                    .findFirst();
            baseline.ifPresent(match -> matches.add(combine(staticMatch, match, hybridRuleCode)));
        }
    }

    private boolean related(BaselineRuleMatch match, String baselineRuleOrMetric) {
        return baselineRuleOrMetric.equals(match.ruleCode()) || baselineRuleOrMetric.equals(match.metric());
    }

    private boolean sameScope(StaticRuleMatch staticMatch, BaselineRuleMatch baselineMatch) {
        if (staticMatch.scopeType() == baselineMatch.scopeType() && staticMatch.scopeKey().equals(baselineMatch.scopeKey())) {
            return true;
        }
        return staticMatch.event() != null && baselineMatch.event() != null
                && equals(staticMatch.event().getEndpointId(), baselineMatch.event().getEndpointId())
                && equals(staticMatch.event().getClientId(), baselineMatch.event().getClientId());
    }

    private AnomalyRuleMatch combine(StaticRuleMatch staticMatch, BaselineRuleMatch baselineMatch, String hybridRuleCode) {
        AnomalySeverity severity = upgrade(max(staticMatch.severity(), baselineMatch.severity()));
        return new AnomalyRuleMatch(null, hybridRuleCode, "HYBRID", severity, baselineMatch.metric(), baselineMatch.scopeType(),
                baselineMatch.scopeKey(), baselineMatch.identity(), baselineMatch.currentValue(), baselineMatch.baselineValue(),
                baselineMatch.thresholdValue(), baselineMatch.windowSeconds(), baselineMatch.timeBucket(), staticMatch.ruleCode(),
                baselineMatch.ruleCode(), baselineMatch.notificationRuleId(),
                baselineMatch.cooldownMinutes() != null ? baselineMatch.cooldownMinutes() : staticMatch.cooldownMinutes(),
                baselineMatch.windowStart(), baselineMatch.windowEnd(), baselineMatch.event());
    }

    private AnomalySeverity upgrade(AnomalySeverity severity) {
        if (severity == AnomalySeverity.MEDIUM) {
            return AnomalySeverity.HIGH;
        }
        if (severity == AnomalySeverity.HIGH) {
            return AnomalySeverity.CRITICAL;
        }
        return severity == null ? AnomalySeverity.MEDIUM : severity;
    }

    private AnomalySeverity max(AnomalySeverity left, AnomalySeverity right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    private boolean equals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
