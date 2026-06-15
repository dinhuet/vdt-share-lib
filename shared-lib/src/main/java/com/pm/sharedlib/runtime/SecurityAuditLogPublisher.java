package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.sharedlib.config.VdtShareProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class SecurityAuditLogPublisher {

    private static final String DEFAULT_TOPIC = "security.logs";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final VdtShareProperties properties;

    public void publish(SecurityLogEvent event) {
        if (event == null || !properties.getAudit().isEnabled() || !properties.getAudit().getKafka().isEnabled()) {
            return;
        }

        var topic = resolveTopic();
        var key = resolveKey(event);
        try {
            var json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, json).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("security_audit_log_publish_failed topic={} key={} endpointId={}",
                            topic, key, event.getEndpointId(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.warn("security_audit_log_serialize_failed topic={} key={} endpointId={}",
                    topic, key, event.getEndpointId(), e);
        } catch (RuntimeException e) {
            log.warn("security_audit_log_send_failed topic={} key={} endpointId={}",
                    topic, key, event.getEndpointId(), e);
        }
    }

    private String resolveTopic() {
        var configuredTopic = properties.getAudit().getKafka().getTopic();
        return StringUtils.hasText(configuredTopic) ? configuredTopic.trim() : DEFAULT_TOPIC;
    }

    private String resolveKey(SecurityLogEvent event) {
        if (StringUtils.hasText(event.getTraceId())) {
            return event.getTraceId().trim();
        }
        if (StringUtils.hasText(event.getCorrelationId())) {
            return event.getCorrelationId().trim();
        }
        if (event.getEndpointId() != null) {
            return event.getEndpointId().toString();
        }
        if (StringUtils.hasText(event.getServiceName())) {
            return event.getServiceName().trim();
        }
        return null;
    }
}
