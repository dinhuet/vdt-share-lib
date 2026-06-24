package com.pm.be.service.anomaly;

import com.pm.be.dto.anomaly.BaselineRuleDefinition;
import com.pm.be.entity.anomaly.AnomalyBaselineRuleConfigEntity;
import com.pm.be.entity.anomaly.AnomalyRuleEntity;
import com.pm.be.enums.AnomalyRuleType;
import com.pm.be.enums.AnomalyTimeBucketType;
import com.pm.be.repository.anomaly.AnomalyRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BaselineRuleConfigService {
    private final AnomalyRuleRepository anomalyRuleRepository;

    public List<BaselineRuleDefinition> loadEnabledBaselineRules() {
        return anomalyRuleRepository.findEnabledBaselineRules(List.of(AnomalyRuleType.BASELINE, AnomalyRuleType.HYBRID)).stream()
                .filter(rule -> rule.getBaselineConfig() != null)
                .map(this::toDefinition)
                .toList();
    }

    private BaselineRuleDefinition toDefinition(AnomalyRuleEntity rule) {
        AnomalyBaselineRuleConfigEntity config = rule.getBaselineConfig();
        int historyDays = positiveOrDefault(config.getHistoryDays(), 7);
        int percentile = percentile(config.getPercentile());
        int windowSeconds = positiveOrDefault(config.getWindowSeconds(), defaultWindowSeconds(rule.getMetric()));
        AnomalyTimeBucketType bucketType = config.getTimeBucketType() == null ? AnomalyTimeBucketType.SAME_HOUR : config.getTimeBucketType();
        return new BaselineRuleDefinition(rule.getId(), rule.getRuleCode(), rule.getMetric(), rule.getScopeType(), rule.getScopeId(),
                historyDays, bucketType, percentile, positiveOrDefault(config.getMinSampleCount(), 1), windowSeconds);
    }

    private int percentile(BigDecimal value) {
        int percentile = value == null ? 95 : value.intValue();
        return percentile < 1 || percentile > 100 ? 95 : percentile;
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private int defaultWindowSeconds(String metric) {
        return metric != null && metric.endsWith("_1m") ? 60 : 300;
    }
}
