package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pm.sharedlib.config.VdtShareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuditLogPublisherTest {

    @Mock KafkaTemplate<String, String> kafkaTemplate;

    ObjectMapper objectMapper;
    VdtShareProperties properties;
    SecurityAuditLogPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        properties = new VdtShareProperties();
        publisher = new SecurityAuditLogPublisher(kafkaTemplate, objectMapper, properties);
    }

    @Test
    void publish_shouldSkipNullEvent() {
        publisher.publish(null);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publish_shouldSkipWhenAuditDisabled() {
        properties.getAudit().setEnabled(false);

        publisher.publish(event());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publish_shouldSkipWhenAuditKafkaDisabled() {
        properties.getAudit().getKafka().setEnabled(false);

        publisher.publish(event());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publish_shouldSendJsonWithTraceIdAsKey() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event());

        var topicCaptor = ArgumentCaptor.forClass(String.class);
        var keyCaptor = ArgumentCaptor.forClass(String.class);
        var payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("security.logs");
        assertThat(keyCaptor.getValue()).isEqualTo("trace-1");
        assertThat(payloadCaptor.getValue()).contains("\"traceId\":\"trace-1\"");
        assertThat(payloadCaptor.getValue()).contains("\"flowType\":\"INBOUND_HTTP\"");
    }

    @Test
    void publish_shouldUseConfiguredTopicAndFallbackKey() throws Exception {
        properties.getAudit().getKafka().setTopic("custom.security.logs");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        var event = event();
        event.setTraceId(null);
        event.setCorrelationId("corr-1");

        publisher.publish(event);

        verify(kafkaTemplate).send("custom.security.logs", "corr-1", objectMapper.writeValueAsString(event));
    }

    @Test
    void publish_shouldFallbackBlankTopicToDefaultTopic() {
        properties.getAudit().getKafka().setTopic(" ");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event());

        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("security.logs"), anyString(), anyString());
    }

    @Test
    void publish_shouldNotThrowWhenSendThrowsSynchronously() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("kafka down"));

        assertThatCode(() -> publisher.publish(event())).doesNotThrowAnyException();
    }

    @Test
    void publish_shouldNotThrowWhenSendFutureCompletesExceptionally() {
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("broker error"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

        assertThatCode(() -> publisher.publish(event())).doesNotThrowAnyException();
    }

    private SecurityLogEvent event() {
        return SecurityLogEvent.builder()
                .timestamp(Instant.parse("2026-06-15T00:00:00Z"))
                .traceId("trace-1")
                .correlationId("corr-1")
                .serviceName("order-service")
                .endpointId(UUID.randomUUID())
                .endpointName("create-order")
                .flowType("INBOUND_HTTP")
                .direction("INBOUND")
                .protocol("HTTP")
                .method("POST")
                .path("/api/orders")
                .status("SUCCESS")
                .retentionDays(30)
                .retentionBucket("r30")
                .build();
    }
}
