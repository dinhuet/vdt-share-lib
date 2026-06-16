package com.pm.sharedlib.runtime;

import com.pm.sharedlib.config.VdtShareProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaOutboundMetadataEnricherTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void enrich_shouldSkipRegistrationAndAuditTopics() {
        MDC.put("traceId", "trace-1");
        MDC.put("correlationId", "corr-1");
        var enricher = new KafkaOutboundMetadataEnricher(new VdtShareProperties());

        var registration = enricher.enrich(new ProducerRecord<>("vdt.service.registration", "k", "v"));
        var audit = enricher.enrich(new ProducerRecord<>("security.logs", "k", "v"));

        assertThat(registration.headers().lastHeader("X-Trace-Id")).isNull();
        assertThat(audit.headers().lastHeader("X-Trace-Id")).isNull();
    }

    @Test
    void enrich_shouldSkipConfiguredAuditTopicAndEnrichBusinessTopic() {
        MDC.put("traceId", "trace-1");
        var properties = new VdtShareProperties();
        properties.getAudit().getKafka().setTopic("audit.custom");
        var enricher = new KafkaOutboundMetadataEnricher(properties);

        var audit = enricher.enrich(new ProducerRecord<>("audit.custom", "k", "v"));
        var business = enricher.enrich(new ProducerRecord<>("orders.created", "k", "v"));

        assertThat(audit.headers().lastHeader("X-Trace-Id")).isNull();
        assertThat(business.headers().lastHeader("X-Trace-Id")).isNotNull();
    }
}
