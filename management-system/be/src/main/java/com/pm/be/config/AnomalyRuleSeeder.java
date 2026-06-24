package com.pm.be.config;

import com.pm.be.entity.anomaly.AnomalyBaselineRuleConfigEntity;
import com.pm.be.entity.anomaly.AnomalyRuleEntity;
import com.pm.be.entity.anomaly.AnomalyStaticRuleConfigEntity;
import com.pm.be.enums.*;
import com.pm.be.repository.anomaly.AnomalyRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AnomalyRuleSeeder implements ApplicationRunner {
    private static final int DEFAULT_COOLDOWN_MINUTES = 5;

    private final AnomalyRuleRepository anomalyRuleRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedStaticRules();
        seedBaselineRules();
    }

    private void seedStaticRules() {
        List<StaticDefault> defaults = List.of(
                new StaticDefault("AUTH_BRUTE_FORCE", "Auth brute force", "Client/IP failed authentication repeatedly within a short window.", "auth_fail_count", 60, 5, AnomalySeverity.HIGH),
                new StaticDefault("SIGNATURE_ATTACK", "Signature attack", "Invalid HMAC signature attempts repeated by client or source IP.", "signature_fail_count", 60, 3, AnomalySeverity.HIGH),
                new StaticDefault("NONCE_REPLAY_ATTACK", "Nonce replay attack", "Repeated nonce usage indicates replay attack attempts.", "nonce_replay_count", 300, 1, AnomalySeverity.CRITICAL),
                new StaticDefault("PERMISSION_PROBING", "Permission probing", "Client/source IP is repeatedly denied by permission checks.", "permission_denied_count", 300, 5, AnomalySeverity.HIGH),
                new StaticDefault("ACCESS_POLICY_ABUSE", "Access policy abuse", "Actor repeatedly hits blacklist or access policy denies.", "access_policy_denied_count", 300, 3, AnomalySeverity.HIGH),
                new StaticDefault("RATE_LIMIT_ABUSE", "Rate limit abuse", "Client/source IP exceeds rate limits repeatedly.", "rate_limit_exceeded_count", 300, 3, AnomalySeverity.HIGH),
                new StaticDefault("PAYLOAD_SIZE_ABUSE", "Payload size abuse", "Client/source IP sends oversized payloads repeatedly.", "request_too_large_count", 300, 3, AnomalySeverity.MEDIUM),
                new StaticDefault("RESPONSE_SIZE_ANOMALY", "Response size anomaly", "Endpoint returns oversized responses repeatedly.", "response_too_large_count", 300, 3, AnomalySeverity.MEDIUM),
                new StaticDefault("RETRY_EXHAUSTED_SPIKE", "Retry exhausted spike", "Outbound calls exhaust retries repeatedly.", "retry_exhausted_count", 300, 3, AnomalySeverity.HIGH),
                new StaticDefault("SLOW_REQUEST_BURST", "Slow request burst", "Slow requests exceed the configured latency threshold repeatedly.", "slow_request_count", 300, 10, AnomalySeverity.MEDIUM)
        );

        defaults.forEach(rule -> {
            if (!anomalyRuleRepo.existsByRuleCode(rule.ruleCode())) {
                AnomalyRuleEntity entity = baseRule(rule.ruleCode(), rule.name(), rule.description(), AnomalyRuleType.STATIC, rule.metric(), rule.severity());
                entity.setStaticConfig(AnomalyStaticRuleConfigEntity.builder()
                        .rule(entity)
                        .thresholdValue(BigDecimal.valueOf(rule.threshold()))
                        .windowSeconds(rule.windowSeconds())
                        .minSampleCount(rule.threshold())
                        .consecutiveWindows(1)
                        .operator(AnomalyRuleOperator.GTE)
                        .build());
                anomalyRuleRepo.save(entity);
            }
        });
    }

    private void seedBaselineRules() {
        List<BaselineDefault> defaults = List.of(
                new BaselineDefault("TRAFFIC_SPIKE", "Traffic spike", "Request count is higher than historical baseline.", "request_count_1m", 50, 2, AnomalySeverity.MEDIUM, BigDecimal.valueOf(50)),
                new BaselineDefault("ERROR_RATE_SPIKE", "Error rate spike", "Failed/error rate is higher than historical baseline.", "error_rate_5m", 20, 1, AnomalySeverity.HIGH, BigDecimal.valueOf(0.30)),
                new BaselineDefault("DENIED_RATE_SPIKE", "Denied rate spike", "Denied request rate is higher than historical baseline.", "denied_rate_5m", 20, 1, AnomalySeverity.HIGH, BigDecimal.valueOf(0.30)),
                new BaselineDefault("LATENCY_SPIKE", "Latency spike", "Endpoint p95 latency is higher than historical baseline.", "p95_duration_5m", 20, 2, AnomalySeverity.MEDIUM, BigDecimal.valueOf(1000)),
                new BaselineDefault("TIMEOUT_RATE_SPIKE", "Timeout rate spike", "Timeout rate is higher than historical baseline.", "timeout_rate_5m", 20, 1, AnomalySeverity.HIGH, BigDecimal.valueOf(0.05)),
                new BaselineDefault("RETRY_RATE_SPIKE", "Retry rate spike", "Outbound retry rate is higher than historical baseline.", "retry_rate_5m", 20, 2, AnomalySeverity.MEDIUM, BigDecimal.valueOf(0.10)),
                new BaselineDefault("AUTH_FAIL_RATE_SPIKE", "Auth fail rate spike", "Authentication failure rate is higher than historical baseline.", "auth_fail_rate_5m", 20, 1, AnomalySeverity.HIGH, BigDecimal.valueOf(0.20))
        );

        defaults.forEach(rule -> {
            if (!anomalyRuleRepo.existsByRuleCode(rule.ruleCode())) {
                AnomalyRuleEntity entity = baseRule(rule.ruleCode(), rule.name(), rule.description(), AnomalyRuleType.BASELINE, rule.metric(), rule.severity());
                entity.setBaselineConfig(AnomalyBaselineRuleConfigEntity.builder()
                        .rule(entity)
                        .historyDays(7)
                        .timeBucketType(AnomalyTimeBucketType.SAME_HOUR)
                        .percentile(BigDecimal.valueOf(95))
                        .multiplier(BigDecimal.valueOf(2))
                        .minAbsoluteThreshold(rule.minAbsoluteThreshold())
                        .minSampleCount(rule.minSampleCount())
                        .consecutiveWindows(rule.consecutiveWindows())
                        .build());
                anomalyRuleRepo.save(entity);
            }
        });
    }

    private AnomalyRuleEntity baseRule(
            String ruleCode,
            String name,
            String description,
            AnomalyRuleType ruleType,
            String metric,
            AnomalySeverity severity) {
        return AnomalyRuleEntity.builder()
                .ruleCode(ruleCode)
                .name(name)
                .description(description)
                .ruleType(ruleType)
                .metric(metric)
                .severity(severity)
                .scopeType(AnomalyScopeType.GLOBAL)
                .enabled(true)
                .cooldownMinutes(DEFAULT_COOLDOWN_MINUTES)
                .build();
    }

    private record StaticDefault(
            String ruleCode,
            String name,
            String description,
            String metric,
            int windowSeconds,
            int threshold,
            AnomalySeverity severity) {
    }

    private record BaselineDefault(
            String ruleCode,
            String name,
            String description,
            String metric,
            int minSampleCount,
            int consecutiveWindows,
            AnomalySeverity severity,
            BigDecimal minAbsoluteThreshold) {
    }
}
