package com.pm.sharedlib.runtime;

import com.pm.sharedlib.config.VdtShareProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public class KafkaOutboundMetadataEnricher {

    private static final String REGISTRATION_TOPIC = "vdt.service.registration";
    private static final String DEFAULT_AUDIT_TOPIC = "security.logs";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final VdtShareProperties properties;

    public KafkaOutboundMetadataEnricher() {
        this(null);
    }

    public KafkaOutboundMetadataEnricher(VdtShareProperties properties) {
        this.properties = properties;
    }

    public <K, V> ProducerRecord<K, V> enrich(ProducerRecord<K, V> record) {
        if (record == null || isExcludedTopic(record.topic())) {
            return record;
        }

        addHeaderIfPresent(record, TRACE_ID_HEADER, MDC.get(TRACE_ID_MDC_KEY));
        addHeaderIfPresent(record, CORRELATION_ID_HEADER, MDC.get(CORRELATION_ID_MDC_KEY));
        return record;
    }

    private boolean isExcludedTopic(String topic) {
        if (REGISTRATION_TOPIC.equals(topic) || DEFAULT_AUDIT_TOPIC.equals(topic)) {
            return true;
        }
        if (properties == null || properties.getAudit() == null || properties.getAudit().getKafka() == null) {
            return false;
        }
        var configuredTopic = properties.getAudit().getKafka().getTopic();
        return StringUtils.hasText(configuredTopic) && configuredTopic.trim().equals(topic);
    }

    private <K, V> void addHeaderIfPresent(ProducerRecord<K, V> record, String headerName, String value) {
        if (StringUtils.hasText(value)) {
            record.headers().remove(headerName);
            record.headers().add(headerName, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
