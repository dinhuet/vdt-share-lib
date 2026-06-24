package com.pm.be.service.anomaly;

import com.pm.be.dto.anomaly.MetricExtractionResult;
import com.pm.be.dto.anomaly.SecurityAnomalyEvent;
import com.pm.be.dto.anomaly.SecurityLogEventMessage;
import com.pm.be.dto.anomaly.StaticRuleMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaticRuleProcessor {
    private final StaticRuleEngine staticRuleEngine;
    private final SecurityAlertService securityAlertService;
    private final SecurityAnomalyEventPublisher securityAnomalyEventPublisher;

    public List<StaticRuleMatch> process(SecurityLogEventMessage event, MetricExtractionResult extractionResult) {
        List<StaticRuleMatch> matches = staticRuleEngine.evaluate(event, extractionResult);
        for (StaticRuleMatch match : matches) {
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
        return matches;
    }
}
