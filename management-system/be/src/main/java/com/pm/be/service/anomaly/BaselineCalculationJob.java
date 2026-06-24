package com.pm.be.service.anomaly;

import com.pm.be.config.BaselineJobProperties;
import com.pm.be.dto.anomaly.BaselineRuleDefinition;
import com.pm.be.dto.anomaly.BaselineUpsertRequest;
import com.pm.be.enums.AnomalyScopeType;
import com.pm.be.enums.AnomalyTimeBucketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaselineCalculationJob {
    private final BaselineJobProperties properties;
    private final BaselineRuleConfigService ruleConfigService;
    private final ElasticsearchBaselineQueryService queryService;
    private final BaselineCalculator calculator;
    private final AnomalyBaselineService baselineService;

    @Scheduled(cron = "${vdt.anomaly.baseline-job.cron:0 5 * * * *}")
    public void run() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            log.debug("Anomaly baseline job disabled");
            return;
        }
        Instant now = Instant.now();
        List<BaselineRuleDefinition> rules = ruleConfigService.loadEnabledBaselineRules();
        log.info("Starting anomaly baseline calculation rules={}", rules.size());
        for (BaselineRuleDefinition rule : rules) {
            try {
                calculateRule(rule, now);
            } catch (RuntimeException e) {
                log.warn("Failed to calculate baseline for ruleCode={} metric={}: {}", rule.ruleCode(), rule.metric(), e.getMessage());
            }
        }
        log.info("Finished anomaly baseline calculation rules={}", rules.size());
    }

    private void calculateRule(BaselineRuleDefinition rule, Instant now) {
        for (AnomalyScopeType scopeType : scopesFor(rule)) {
            for (String timeBucket : timeBuckets(rule.timeBucketType(), now)) {
                Map<String, List<Double>> valuesByScope = queryService.queryBucketValues(rule.metric(), scopeType, rule.historyDays(),
                        rule.timeBucketType(), timeBucket, rule.windowSeconds(), now);
                valuesByScope.forEach((scopeKey, values) -> calculator.percentile(values, rule.percentile())
                        .ifPresent(result -> {
                            try {
                                baselineService.upsert(new BaselineUpsertRequest(rule.ruleId(), rule.metric(), scopeType, scopeKey,
                                        rule.timeBucketType(), timeBucket, rule.historyDays(), rule.percentile(), "P" + rule.percentile(),
                                        result.value(), result.sampleCount(), LocalDateTime.now(), rule.windowSeconds()));
                            } catch (RuntimeException e) {
                                log.warn("Failed to upsert baseline ruleCode={} metric={} scopeType={} scopeKey={}: {}",
                                        rule.ruleCode(), rule.metric(), scopeType, scopeKey, e.getMessage());
                            }
                        }));
            }
        }
    }

    private List<AnomalyScopeType> scopesFor(BaselineRuleDefinition rule) {
        if (rule.configuredScopeType() == AnomalyScopeType.ENDPOINT || rule.configuredScopeType() == AnomalyScopeType.ENDPOINT_CLIENT) {
            return List.of(rule.configuredScopeType());
        }
        if ("auth_fail_rate_5m".equals(rule.metric())) {
            return List.of(AnomalyScopeType.ENDPOINT_CLIENT, AnomalyScopeType.ENDPOINT);
        }
        return List.of(AnomalyScopeType.ENDPOINT, AnomalyScopeType.ENDPOINT_CLIENT);
    }

    private List<String> timeBuckets(AnomalyTimeBucketType type, Instant now) {
        if (type == AnomalyTimeBucketType.GLOBAL) {
            return List.of("GLOBAL");
        }
        int hour = now.atZone(java.time.ZoneId.systemDefault()).getHour();
        return List.of("HOUR_%02d".formatted(hour), "GLOBAL");
    }
}
