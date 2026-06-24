package com.pm.be.service.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.pm.be.config.StaticRuleProperties;
import com.pm.be.dto.anomaly.SecurityAnomalyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAnomalyEventPublisherTest {
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    private SecurityAnomalyEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        publisher = new SecurityAnomalyEventPublisher(kafkaTemplate, objectMapper, new StaticRuleProperties());
    }

    @Test
    void publish_shouldUseConfiguredTopicAndFingerprintKey() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(SecurityAnomalyEvent.builder().fingerprint("fp-1").build());

        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("security.anomalies"), org.mockito.ArgumentMatchers.eq("fp-1"), anyString());
    }

    @Test
    void publish_kafkaThrows_shouldSwallowFailure() {
        doThrow(new RuntimeException("kafka down")).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        publisher.publish(SecurityAnomalyEvent.builder().fingerprint("fp-1").build());
    }
}
