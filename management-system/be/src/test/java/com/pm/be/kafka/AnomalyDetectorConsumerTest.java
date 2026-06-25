package com.pm.be.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.pm.be.service.anomaly.MetricCounterService;
import com.pm.be.service.anomaly.MetricExtractionService;
import com.pm.be.service.anomaly.BaselineRuleProcessor;
import com.pm.be.service.anomaly.StaticRuleProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectorConsumerTest {
    @Mock MetricExtractionService metricExtractionService;
    @Mock MetricCounterService metricCounterService;
    @Mock StaticRuleProcessor staticRuleProcessor;
    @Mock BaselineRuleProcessor baselineRuleProcessor;

    private AnomalyDetectorConsumer consumer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        consumer = new AnomalyDetectorConsumer(objectMapper, metricExtractionService, metricCounterService, staticRuleProcessor, baselineRuleProcessor);
    }

    @Test
    void handle_invalidJson_shouldSkipWithoutCallingServices() {
        consumer.handle("{invalid-json");

        verify(metricExtractionService, never()).extract(any());
        verify(metricCounterService, never()).increment(any());
    }

    @Test
    void handle_missingTimestampWithoutFallback_shouldSkipWithoutCallingServices() {
        consumer.handle("{\"endpointId\":\"endpoint-1\",\"flowType\":\"INBOUND_HTTP\"}");

        verify(metricExtractionService, never()).extract(any());
        verify(metricCounterService, never()).increment(any());
    }

    @Test
    void handle_payloadWithUnknownAuditFields_shouldProcessEvent() {
        consumer.handle("""
                {
                  "timestamp":"2026-06-25T04:26:27.486100800Z",
                  "traceId":null,
                  "correlationId":null,
                  "serviceName":"order-service",
                  "endpointId":"6049f03a-c4ee-3689-840e-f666f133dcc5",
                  "endpointName":"get-orders",
                  "flowType":"INBOUND_HTTP",
                  "direction":"INBOUND",
                  "protocol":"HTTP",
                  "method":"GET",
                  "path":"/api/orders",
                  "clientId":"79d0f072-3017-4fc2-aff5-dd7ceda614d4",
                  "sourceIp":"0:0:0:0:0:0:0:1",
                  "status":"DENIED",
                  "resultCode":"AUTH_SIGNATURE_INVALID",
                  "errorCode":"AUTH_SIGNATURE_INVALID",
                  "denyReason":"Invalid request signature",
                  "durationMs":8,
                  "latencyThresholdMs":1000,
                  "timeoutMs":30000,
                  "retryDelayMs":null,
                  "failureAction":null,
                  "retentionDays":30,
                  "retentionBucket":"r30"
                }
                """);

        verify(metricExtractionService).extract(any());
        verify(metricCounterService).increment(any());
    }
}
