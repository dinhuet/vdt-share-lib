package com.pm.be.service.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.config.JacksonConfig;
import com.pm.be.config.StaticRuleProperties;
import com.pm.be.dto.anomaly.SecurityAnomalyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAnomalyEventPublisherTest {
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    private SecurityAnomalyEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new JacksonConfig().objectMapper();
        publisher = new SecurityAnomalyEventPublisher(kafkaTemplate, objectMapper, new StaticRuleProperties());
    }

    @Test
    void publish_shouldUseConfiguredTopicAndFingerprintKey() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(SecurityAnomalyEvent.builder().fingerprint("fp-1").build());

        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("security.anomalies"), org.mockito.ArgumentMatchers.eq("fp-1"), anyString());
    }

    @Test
    void publish_shouldSerializeInstantsAsIso8601Strings() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(SecurityAnomalyEvent.builder()
                .fingerprint("fp-1")
                .timestamp(Instant.parse("2026-06-24T10:28:09.151Z"))
                .windowStart(Instant.parse("2026-06-24T10:28:00Z"))
                .windowEnd(Instant.parse("2026-06-24T10:29:00Z"))
                .build());

        verify(kafkaTemplate).send(
                eq("security.anomalies"),
                eq("fp-1"),
                contains("\"timestamp\":\"2026-06-24T10:28:09.151Z\""));
        verify(kafkaTemplate).send(
                eq("security.anomalies"),
                eq("fp-1"),
                contains("\"windowStart\":\"2026-06-24T10:28:00Z\""));
        verify(kafkaTemplate).send(
                eq("security.anomalies"),
                eq("fp-1"),
                contains("\"windowEnd\":\"2026-06-24T10:29:00Z\""));
    }

    @Test
    void publish_kafkaThrows_shouldSwallowFailure() {
        doThrow(new RuntimeException("kafka down")).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        publisher.publish(SecurityAnomalyEvent.builder().fingerprint("fp-1").build());
    }
}
