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
                new StaticDefault("AUTH_BRUTE_FORCE", "Auth brute force", "Client failed authentication repeatedly within a short window.", "auth_fail_count", AnomalyScopeType.ENDPOINT_CLIENT, 60, 5, AnomalySeverity.HIGH),
                new StaticDefault("AUTH_BRUTE_FORCE_IP", "Auth brute force by IP", "Source IP failed authentication repeatedly within a short window.", "auth_fail_count", AnomalyScopeType.ENDPOINT_IP, 60, 5, AnomalySeverity.HIGH),
                new StaticDefault("SIGNATURE_ATTACK", "Signature attack", "Client sends invalid HMAC signatures repeatedly.", "signature_fail_count", AnomalyScopeType.ENDPOINT_CLIENT, 60, 3, AnomalySeverity.HIGH),
                new StaticDefault("SIGNATURE_ATTACK_IP", "Signature attack by IP", "Source IP sends invalid HMAC signatures repeatedly.", "signature_fail_count", AnomalyScopeType.ENDPOINT_IP, 60, 3, AnomalySeverity.HIGH),
                new StaticDefault("NONCE_REPLAY_ATTACK", "Nonce replay attack", "Client repeats nonce usage, indicating replay attack attempts.", "nonce_replay_count", AnomalyScopeType.ENDPOINT_CLIENT, 300, 1, AnomalySeverity.CRITICAL),
                new StaticDefault("NONCE_REPLAY_ATTACK_IP", "Nonce replay attack by IP", "Source IP repeats nonce usage, indicating replay attack attempts.", "nonce_replay_count", AnomalyScopeType.ENDPOINT_IP, 300, 1, AnomalySeverity.CRITICAL),
                new StaticDefault("PERMISSION_PROBING", "Permission probing", "Client is repeatedly denied by permission checks.", "permission_denied_count", AnomalyScopeType.ENDPOINT_CLIENT, 300, 5, AnomalySeverity.HIGH),
                new StaticDefault("PERMISSION_PROBING_IP", "Permission probing by IP", "Source IP is repeatedly denied by permission checks.", "permission_denied_count", AnomalyScopeType.ENDPOINT_IP, 300, 5, AnomalySeverity.HIGH),
                new StaticDefault("ACCESS_POLICY_ABUSE", "Access policy abuse", "Client repeatedly hits blacklist or access policy denies.", "access_policy_denied_count", AnomalyScopeType.ENDPOINT_CLIENT, 300, 3, AnomalySeverity.HIGH),
                new StaticDefault("ACCESS_POLICY_ABUSE_IP", "Access policy abuse by IP", "Source IP repeatedly hits blacklist or access policy denies.", "access_policy_denied_count", AnomalyScopeType.ENDPOINT_IP, 300, 3, AnomalySeverity.HIGH),
                new StaticDefault("RATE_LIMIT_ABUSE", "Rate limit abuse", "Client exceeds rate limits repeatedly.", "rate_limit_exceeded_count", AnomalyScopeType.ENDPOINT_CLIENT, 300, 3, AnomalySeverity.HIGH),
                new StaticDefault("RATE_LIMIT_ABUSE_IP", "Rate limit abuse by IP", "Source IP exceeds rate limits repeatedly.", "rate_limit_exceeded_count", AnomalyScopeType.ENDPOINT_IP, 300, 3, AnomalySeverity.HIGH),
                new StaticDefault("PAYLOAD_SIZE_ABUSE", "Payload size abuse", "Client sends oversized payloads repeatedly.", "request_too_large_count", AnomalyScopeType.ENDPOINT_CLIENT, 300, 3, AnomalySeverity.MEDIUM),
                new StaticDefault("PAYLOAD_SIZE_ABUSE_IP", "Payload size abuse by IP", "Source IP sends oversized payloads repeatedly.", "request_too_large_count", AnomalyScopeType.ENDPOINT_IP, 300, 3, AnomalySeverity.MEDIUM),
                new StaticDefault("RESPONSE_SIZE_ANOMALY", "Response size anomaly", "Endpoint returns oversized responses repeatedly.", "response_too_large_count", AnomalyScopeType.ENDPOINT, 300, 3, AnomalySeverity.MEDIUM),
                new StaticDefault("RETRY_EXHAUSTED_SPIKE", "Retry exhausted spike", "Endpoint outbound calls exhaust retries repeatedly.", "retry_exhausted_count", AnomalyScopeType.ENDPOINT, 300, 3, AnomalySeverity.HIGH),
                new StaticDefault("SLOW_REQUEST_BURST", "Slow request burst", "Endpoint requests exceed the configured latency threshold repeatedly.", "slow_request_count", AnomalyScopeType.ENDPOINT, 300, 10, AnomalySeverity.MEDIUM)
        );

        defaults.forEach(this::upsertStaticRule);
    }

    private void upsertStaticRule(StaticDefault rule) {
        AnomalyRuleEntity entity = anomalyRuleRepo.findByRuleCode(rule.ruleCode())
                .orElseGet(() -> baseRule(rule.ruleCode(), rule.name(), rule.description(), AnomalyRuleType.STATIC, rule.metric(), rule.severity(), rule.scopeType()));
        entity.setName(rule.name());
        entity.setDescription(rule.description());
        entity.setRuleType(AnomalyRuleType.STATIC);
        entity.setMetric(rule.metric());
        entity.setSeverity(rule.severity());
        entity.setScopeType(rule.scopeType());
        entity.setCooldownMinutes(DEFAULT_COOLDOWN_MINUTES);
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        if (entity.getStaticConfig() == null) {
            entity.setStaticConfig(AnomalyStaticRuleConfigEntity.builder().rule(entity).build());
        }
        entity.getStaticConfig().setRule(entity);
        entity.getStaticConfig().setThresholdValue(BigDecimal.valueOf(rule.threshold()));
        entity.getStaticConfig().setWindowSeconds(rule.windowSeconds());
        entity.getStaticConfig().setMinSampleCount(rule.threshold());
        entity.getStaticConfig().setConsecutiveWindows(1);
        entity.getStaticConfig().setOperator(AnomalyRuleOperator.GTE);
        anomalyRuleRepo.save(entity);
    }

    private void seedBaselineRules() {
        List<BaselineDefault> defaults = List.of(
                new BaselineDefault("TRAFFIC_SPIKE", "Traffic spike", "Request count is higher than historical baseline.", "request_count_1m", 50, 2, AnomalySeverity.MEDIUM, BigDecimal.valueOf(50)),
                new BaselineDefault("ERROR_RATE_SPIKE", "Error rate spike", "Failed/error rate is higher than historical baseline.", "error_rate_5m", 20, 1, AnomalySeverity.HIGH, BigDecimal.valueOf(0.30)),
                new BaselineDefault("DENIED_RATE_SPIKE", "Denied rate spike", "Denied request rate is higher than historical baseline.", "denied_rate_5m", 20, 1, AnomalySeverity.HIGH, BigDecimal.valueOf(0.30)),
                new BaselineDefault("LATENCY_SPIKE", "Latency spike", "Slow request rate is higher than historical baseline.", "slow_request_rate_5m", 20, 2, AnomalySeverity.MEDIUM, BigDecimal.valueOf(0.20)),
                new BaselineDefault("TIMEOUT_RATE_SPIKE", "Timeout rate spike", "Timeout rate is higher than historical baseline.", "timeout_rate_5m", 20, 1, AnomalySeverity.HIGH, BigDecimal.valueOf(0.05)),
                new BaselineDefault("RETRY_RATE_SPIKE", "Retry rate spike", "Outbound retry rate is higher than historical baseline.", "retry_rate_5m", 20, 2, AnomalySeverity.MEDIUM, BigDecimal.valueOf(0.10)),
                new BaselineDefault("AUTH_FAIL_RATE_SPIKE", "Auth fail rate spike", "Authentication failure rate is higher than historical baseline.", "auth_fail_rate_5m", 20, 1, AnomalySeverity.HIGH, BigDecimal.valueOf(0.20))
        );

        defaults.forEach(this::upsertBaselineRule);
    }

    private void upsertBaselineRule(BaselineDefault rule) {
        AnomalyRuleEntity entity = anomalyRuleRepo.findByRuleCode(rule.ruleCode())
                .orElseGet(() -> baseRule(rule.ruleCode(), rule.name(), rule.description(), AnomalyRuleType.BASELINE, rule.metric(), rule.severity(), AnomalyScopeType.GLOBAL));
        entity.setName(rule.name());
        entity.setDescription(rule.description());
        entity.setRuleType(AnomalyRuleType.BASELINE);
        entity.setMetric(rule.metric());
        entity.setSeverity(rule.severity());
        entity.setScopeType(AnomalyScopeType.GLOBAL);
        entity.setCooldownMinutes(DEFAULT_COOLDOWN_MINUTES);
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        if (entity.getBaselineConfig() == null) {
            entity.setBaselineConfig(AnomalyBaselineRuleConfigEntity.builder().rule(entity).build());
        }
        entity.getBaselineConfig().setRule(entity);
        entity.getBaselineConfig().setHistoryDays(7);
        entity.getBaselineConfig().setTimeBucketType(AnomalyTimeBucketType.SAME_HOUR);
        entity.getBaselineConfig().setPercentile(BigDecimal.valueOf(95));
        entity.getBaselineConfig().setMultiplier(BigDecimal.valueOf(2));
        entity.getBaselineConfig().setMinAbsoluteThreshold(rule.minAbsoluteThreshold());
        entity.getBaselineConfig().setMinSampleCount(rule.minSampleCount());
        entity.getBaselineConfig().setConsecutiveWindows(rule.consecutiveWindows());
        entity.getBaselineConfig().setWindowSeconds(windowSeconds(rule.metric()));
        anomalyRuleRepo.save(entity);
    }

    private int windowSeconds(String metric) {
        return metric != null && metric.endsWith("_1m") ? 60 : 300;
    }

    private AnomalyRuleEntity baseRule(
            String ruleCode,
            String name,
            String description,
            AnomalyRuleType ruleType,
            String metric,
            AnomalySeverity severity,
            AnomalyScopeType scopeType) {
        return AnomalyRuleEntity.builder()
                .ruleCode(ruleCode)
                .name(name)
                .description(description)
                .ruleType(ruleType)
                .metric(metric)
                .severity(severity)
                .scopeType(scopeType)
                .enabled(true)
                .cooldownMinutes(DEFAULT_COOLDOWN_MINUTES)
                .build();
    }

    private record StaticDefault(
            String ruleCode,
            String name,
            String description,
            String metric,
            AnomalyScopeType scopeType,
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
