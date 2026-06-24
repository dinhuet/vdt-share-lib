package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyDetectorProperties;
import com.pm.be.dto.anomaly.MetricIncrement;
import com.pm.be.dto.anomaly.SecurityLogEventMessage;
import com.pm.be.enums.AnomalyScopeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MetricExtractionServiceTest {
    private MetricExtractionService service;

    @BeforeEach
    void setUp() {
        service = new MetricExtractionService(new AnomalyDetectorProperties());
    }

    @Test
    void extract_successEvent_shouldOnlyIncrementRequestCountForEndpointAndClient() {
        var result = service.extract(baseEvent("SUCCESS", null));

        assertThat(metrics(result.metricIncrements())).containsOnly("request_count");
        assertThat(result.metricIncrements())
                .extracting(MetricIncrement::scopeType)
                .contains(AnomalyScopeType.ENDPOINT, AnomalyScopeType.ENDPOINT_CLIENT)
                .doesNotContain(AnomalyScopeType.ENDPOINT_IP);
        assertThat(result.metricIncrements())
                .extracting(MetricIncrement::windowSeconds)
                .containsOnly(60, 300);
    }

    @Test
    void extract_authSignatureInvalid_shouldIncrementAuthAndSignatureFailureForClientAndIp() {
        var result = service.extract(baseEvent("DENIED", "AUTH_SIGNATURE_INVALID"));

        assertThat(metrics(result.metricIncrements()))
                .contains("request_count", "denied_count", "auth_fail_count", "signature_fail_count")
                .doesNotContain("success_count");
        assertThat(result.metricIncrements().stream()
                .filter(increment -> "auth_fail_count".equals(increment.metric()))
                .map(MetricIncrement::scopeType))
                .contains(AnomalyScopeType.ENDPOINT_CLIENT, AnomalyScopeType.ENDPOINT_IP);
    }

    @Test
    void extract_nonceReplay_shouldNotIncrementAuthFailCount() {
        var result = service.extract(baseEvent("DENIED", "AUTH_NONCE_REPLAYED"));

        assertThat(metrics(result.metricIncrements()))
                .contains("nonce_replay_count")
                .doesNotContain("auth_fail_count");
    }

    @Test
    void extract_durationAboveThreshold_shouldIncrementSlowRequestCount() {
        var event = baseEvent("SUCCESS", null);
        event.setDurationMs(1_500L);
        event.setLatencyThresholdMs(1_000L);

        var result = service.extract(event);

        assertThat(metrics(result.metricIncrements())).contains("slow_request_count");
    }

    @Test
    void extract_permissionDenied_shouldCreateDistinctDeniedEndpointSetForClientAndIp() {
        var result = service.extract(baseEvent("DENIED", "PERMISSION_DENIED"));

        assertThat(metrics(result.metricIncrements())).contains("permission_denied_count");
        assertThat(result.distinctDeniedEndpointIncrements()).hasSize(2);
        assertThat(result.distinctDeniedEndpointIncrements())
                .extracting(increment -> increment.identityType())
                .containsExactlyInAnyOrder("client", "ip");
    }

    @Test
    void extract_retryAttemptGreaterThanOne_shouldIncrementRetryCount() {
        var event = baseEvent("SUCCESS", null);
        event.setRetryAttempt(2);

        var result = service.extract(event);

        assertThat(metrics(result.metricIncrements())).contains("retry_count");
    }

    private SecurityLogEventMessage baseEvent(String status, String resultCode) {
        var event = new SecurityLogEventMessage();
        event.setTimestamp(Instant.parse("2026-06-23T10:00:01Z"));
        event.setServiceName("order-service");
        event.setEndpointId("endpoint-1");
        event.setFlowType("INBOUND_HTTP");
        event.setClientId("client-a");
        event.setSourceIp("10.0.0.5");
        event.setStatus(status);
        event.setResultCode(resultCode);
        return event;
    }

    private Set<String> metrics(Iterable<MetricIncrement> increments) {
        return ((java.util.Collection<MetricIncrement>) increments).stream()
                .map(MetricIncrement::metric)
                .collect(Collectors.toSet());
    }
}
