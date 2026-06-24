package com.pm.be.service.anomaly;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.config.StaticRuleProperties;
import com.pm.be.dto.anomaly.SecurityAnomalyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAnomalyEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final StaticRuleProperties properties;

    public void publish(SecurityAnomalyEvent event) {
        if (!Boolean.TRUE.equals(properties.getPublishEnabled()) || event == null || !StringUtils.hasText(event.getFingerprint())) {
            return;
        }
        String topic = StringUtils.hasText(properties.getAnomalyTopic()) ? properties.getAnomalyTopic().trim() : "security.anomalies";
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.getFingerprint(), payload)
                    .exceptionally(ex -> {
                        log.warn("Failed to publish security anomaly event: topic={} fingerprint={}", topic, event.getFingerprint(), ex);
                        return null;
                    });
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Failed to publish security anomaly event: topic={} fingerprint={}", topic, event.getFingerprint(), e);
        }
    }
}
