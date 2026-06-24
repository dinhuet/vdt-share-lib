package com.pm.be.service.anomaly;

import com.pm.be.dto.anomaly.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaselineRuleProcessor {
    private final BaselineRuleEngine baselineRuleEngine;
    private final HybridRuleEngine hybridRuleEngine;
    private final SecurityAlertService securityAlertService;
    private final SecurityAnomalyEventPublisher securityAnomalyEventPublisher;

    public void process(SecurityLogEventMessage event, MetricExtractionResult extractionResult, List<StaticRuleMatch> staticMatches) {
        List<BaselineRuleMatch> baselineMatches = baselineRuleEngine.evaluate(event, extractionResult);
        for (BaselineRuleMatch match : baselineMatches) {
            persistAndPublish(AnomalyRuleMatch.fromBaseline(match), event);
        }
        for (AnomalyRuleMatch hybridMatch : hybridRuleEngine.evaluate(staticMatches == null ? List.of() : staticMatches, baselineMatches)) {
            persistAndPublish(hybridMatch, event);
        }
    }

    private void persistAndPublish(AnomalyRuleMatch match, SecurityLogEventMessage event) {
        try {
            SecurityAnomalyEvent anomalyEvent = securityAlertService.createOrUpdate(match);
            try {
                securityAnomalyEventPublisher.publish(anomalyEvent);
            } catch (RuntimeException e) {
                log.warn("Failed to publish anomaly event after alert persisted: ruleCode={} endpointId={}",
                        match.ruleCode(), event == null ? null : event.getEndpointId(), e);
            }
        } catch (RuntimeException e) {
            log.error("Failed to persist security alert; anomaly event will not be published: ruleCode={} endpointId={}",
                    match.ruleCode(), event == null ? null : event.getEndpointId(), e);
        }
    }
}
