package com.pm.be.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.pm.be.service.anomaly.MetricCounterService;
import com.pm.be.service.anomaly.MetricExtractionService;
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

    private AnomalyDetectorConsumer consumer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        consumer = new AnomalyDetectorConsumer(objectMapper, metricExtractionService, metricCounterService);
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
}
