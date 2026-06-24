package com.pm.be.service.anomaly;

import com.pm.be.dto.anomaly.MetricExtractionResult;
import com.pm.be.dto.anomaly.SecurityAnomalyEvent;
import com.pm.be.dto.anomaly.SecurityLogEventMessage;
import com.pm.be.dto.anomaly.StaticRuleMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaticRuleProcessor {
    private final StaticRuleEngine staticRuleEngine;
    private final SecurityAlertService securityAlertService;
    private final SecurityAnomalyEventPublisher securityAnomalyEventPublisher;

    public void process(SecurityLogEventMessage event, MetricExtractionResult extractionResult) {
        for (StaticRuleMatch match : staticRuleEngine.evaluate(event, extractionResult)) {
            try {
                SecurityAnomalyEvent anomalyEvent = securityAlertService.createOrUpdate(match);
                securityAnomalyEventPublisher.publish(anomalyEvent);
            } catch (RuntimeException e) {
                log.error("Failed to persist security alert; anomaly event will not be published: ruleCode={} endpointId={}",
                        match.ruleCode(), event == null ? null : event.getEndpointId(), e);
            }
        }
    }
}
