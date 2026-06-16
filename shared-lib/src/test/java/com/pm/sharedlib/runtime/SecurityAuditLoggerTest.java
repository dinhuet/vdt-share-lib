package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pm.sharedlib.config.VdtShareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityAuditLoggerTest {

    @Mock SecurityAuditLogPublisher publisher;

    ObjectMapper objectMapper;
    VdtShareProperties properties;
    SecurityAuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        properties = new VdtShareProperties();
        auditLogger = new SecurityAuditLogger(objectMapper, properties, publisher);
    }

    @Test
    void log_shouldSkipNullEvent() {
        auditLogger.log(null);

        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void log_shouldSkipWhenAuditDisabled() {
        properties.getAudit().setEnabled(false);
        var event = event();

        auditLogger.log(event);

        verify(publisher, never()).publish(event);
    }

    @Test
    void log_shouldPublishWhenEnabled() {
        var event = event();

        auditLogger.log(event);

        verify(publisher).publish(event);
    }

    @Test
    void log_shouldPublishWhenLocalLogDisabled() {
        properties.getAudit().setLogLocal(false);
        var event = event();

        auditLogger.log(event);

        verify(publisher).publish(event);
    }

    @Test
    void log_shouldNotThrowWhenPublisherThrows() {
        var event = event();
        doThrow(new RuntimeException("publisher failed")).when(publisher).publish(event);

        assertThatCode(() -> auditLogger.log(event)).doesNotThrowAnyException();
        verify(publisher).publish(event);
    }

    @Test
    void log_shouldNotThrowAndStillPublishWhenLocalSerializationFails() throws Exception {
        var mapper = org.mockito.Mockito.mock(ObjectMapper.class);
        var logger = new SecurityAuditLogger(mapper, properties, publisher);
        var event = event();
        org.mockito.Mockito.when(mapper.writeValueAsString(event))
                .thenThrow(new TestJsonProcessingException("cannot serialize"));

        assertThatCode(() -> logger.log(event)).doesNotThrowAnyException();
        verify(publisher).publish(event);
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

    private static class TestJsonProcessingException extends JsonProcessingException {
        TestJsonProcessingException(String message) {
            super(message);
        }
    }
}
