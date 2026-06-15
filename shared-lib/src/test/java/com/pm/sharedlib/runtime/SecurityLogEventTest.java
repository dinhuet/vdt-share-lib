package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityLogEventTest {

    @Test
    void shouldSerializeCoreFieldsToJson() throws Exception {
        var endpointId = UUID.randomUUID();
        var event = SecurityLogEvent.builder()
                .timestamp(Instant.parse("2026-06-15T00:00:00Z"))
                .traceId("trace-1")
                .correlationId("corr-1")
                .serviceName("order-service")
                .endpointId(endpointId)
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

        var json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(event);

        assertThat(json).contains("\"traceId\":\"trace-1\"");
        assertThat(json).contains("\"endpointId\":\"" + endpointId + "\"");
        assertThat(json).contains("\"flowType\":\"INBOUND_HTTP\"");
        assertThat(json).contains("\"retentionBucket\":\"r30\"");
    }
}
