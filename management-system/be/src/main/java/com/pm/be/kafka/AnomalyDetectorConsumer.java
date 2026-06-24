package com.pm.be.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.dto.anomaly.MetricExtractionResult;
import com.pm.be.dto.anomaly.SecurityLogEventMessage;
import com.pm.be.dto.anomaly.StaticRuleMatch;
import com.pm.be.service.anomaly.BaselineRuleProcessor;
import com.pm.be.service.anomaly.MetricCounterService;
import com.pm.be.service.anomaly.MetricExtractionService;
import com.pm.be.service.anomaly.StaticRuleProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "vdt.anomaly.detector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AnomalyDetectorConsumer {
    private final ObjectMapper objectMapper;
    private final MetricExtractionService metricExtractionService;
    private final MetricCounterService metricCounterService;
    private final StaticRuleProcessor staticRuleProcessor;
    private final BaselineRuleProcessor baselineRuleProcessor;

    @KafkaListener(
            topics = "${vdt.anomaly.detector.kafka.topic:security.logs}",
            groupId = "${vdt.anomaly.detector.kafka.group-id:security-anomaly-detector}")
    public void handle(ConsumerRecord<String, String> record) {
        if (record == null) {
            log.warn("Skip security log event because Kafka record is null");
            return;
        }
        handle(record.value(), record.timestamp());
    }

    public void handle(String eventJson) {
        handle(eventJson, null);
    }

    public void handle(String eventJson, Long fallbackTimestampMs) {
        if (!StringUtils.hasText(eventJson)) {
            log.warn("Skip security log event because payload is empty");
            return;
        }

        try {
            SecurityLogEventMessage event = objectMapper.readValue(eventJson, SecurityLogEventMessage.class);
            if (event.getTimestamp() == null && fallbackTimestampMs != null && fallbackTimestampMs > 0) {
                event.setTimestamp(Instant.ofEpochMilli(fallbackTimestampMs));
            }
            if (!isValid(event)) {
                log.warn("Skip invalid security log event: timestamp={} endpointId={} serviceName={} flowType={}",
                        event.getTimestamp(), event.getEndpointId(), event.getServiceName(), event.getFlowType());
                return;
            }

            MetricExtractionResult extractionResult = metricExtractionService.extract(event);
            metricCounterService.increment(extractionResult);
            List<StaticRuleMatch> staticMatches = staticRuleProcessor.process(event, extractionResult);
            baselineRuleProcessor.process(event, extractionResult, staticMatches);
        } catch (JsonProcessingException e) {
            log.warn("Skip security log event because JSON parsing failed", e);
        } catch (RuntimeException e) {
            log.warn("Failed to process security log event; message skipped", e);
        }
    }

    private boolean isValid(SecurityLogEventMessage event) {
        return event != null
                && event.getTimestamp() != null
                && (StringUtils.hasText(event.getEndpointId())
                || StringUtils.hasText(event.getServiceName())
                || StringUtils.hasText(event.getFlowType()));
    }
}
