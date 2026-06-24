package com.pm.be.service.anomaly;

import com.pm.be.config.AnomalyDetectorProperties;
import com.pm.be.dto.anomaly.DistinctDeniedEndpointIncrement;
import com.pm.be.dto.anomaly.MetricExtractionResult;
import com.pm.be.dto.anomaly.MetricIncrement;
import com.pm.be.dto.anomaly.SecurityLogEventMessage;
import com.pm.be.enums.AnomalyScopeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricExtractionService {
    private static final String REQUEST_COUNT = "request_count";
    private static final String FAILED_COUNT = "failed_count";
    private static final String DENIED_COUNT = "denied_count";
    private static final String TIMEOUT_COUNT = "timeout_count";
    private static final String RETRY_COUNT = "retry_count";
    private static final String SLOW_REQUEST_COUNT = "slow_request_count";
    private static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    private static final List<Integer> SECURITY_FAST_WINDOWS = List.of(60, 300);
    private static final List<Integer> SINGLE_300_WINDOW = List.of(300);

    private final AnomalyDetectorProperties properties;

    public MetricExtractionResult extract(SecurityLogEventMessage event) {
        if (event == null || event.getTimestamp() == null) {
            return MetricExtractionResult.empty();
        }

        NormalizedEvent normalized = normalize(event);
        if (!hasAnyScopeAnchor(normalized)) {
            return MetricExtractionResult.empty();
        }

        Set<MetricIncrement> increments = new LinkedHashSet<>();
        addRequestCount(increments, normalized);
        addStatusCounters(increments, normalized);
        addResultCodeCounters(increments, normalized);
        addDurationCounters(increments, normalized);

        List<DistinctDeniedEndpointIncrement> distinctSets = buildDistinctDeniedEndpointIncrements(normalized);
        log.debug("metric_extracted event endpointId={} resultCode={} metrics={} distinctSets={}",
                normalized.endpointId(), normalized.resultCode(), increments.size(), distinctSets.size());
        return new MetricExtractionResult(List.copyOf(increments), distinctSets);
    }

    private void addRequestCount(Set<MetricIncrement> increments, NormalizedEvent event) {
        List<Scope> scopes = event.isSuccess()
                ? successScopes(event)
                : nonSuccessScopes(event);
        addMetric(increments, REQUEST_COUNT, windows(properties.getSuccessWindows(), List.of(60, 300)), scopes, event);
    }

    private void addStatusCounters(Set<MetricIncrement> increments, NormalizedEvent event) {
        switch (event.status()) {
            case "DENIED" -> addMetric(increments, DENIED_COUNT, violationWindows(), securityScopes(event), event);
            case "FAILED" -> addMetric(increments, FAILED_COUNT, violationWindows(), failureScopes(event), event);
            case "TIMEOUT" -> {
                addMetric(increments, TIMEOUT_COUNT, violationWindows(), failureScopes(event), event);
                addMetric(increments, FAILED_COUNT, violationWindows(), failureScopes(event), event);
            }
            case "RETRY" -> addMetric(increments, RETRY_COUNT, violationWindows(), failureScopes(event), event);
            default -> {
            }
        }

        if (event.retryAttempt() != null && event.retryAttempt() > 1) {
            addMetric(increments, RETRY_COUNT, violationWindows(), failureScopes(event), event);
        }
        if (!TIMEOUT_COUNT.equals(metricForTimeout(event)) && isDurationTimeout(event)) {
            addMetric(increments, TIMEOUT_COUNT, violationWindows(), failureScopes(event), event);
        }
    }

    private void addResultCodeCounters(Set<MetricIncrement> increments, NormalizedEvent event) {
        String resultCode = event.resultCode();
        if (!StringUtils.hasText(resultCode)) {
            resultCode = event.errorCode();
        }
        if (!StringUtils.hasText(resultCode)) {
            return;
        }

        switch (resultCode) {
            case "AUTH_SIGNATURE_INVALID", "AUTH_SIGNATURE_HEADER_MISSING", "AUTH_ALGORITHM_UNSUPPORTED",
                 "AUTH_SIGNING_SECRET_INVALID" -> {
                addMetric(increments, "auth_fail_count", SECURITY_FAST_WINDOWS, securityScopes(event), event);
                addMetric(increments, "signature_fail_count", SECURITY_FAST_WINDOWS, securityScopes(event), event);
            }
            case "AUTH_NONCE_REPLAYED" -> addMetric(increments, "nonce_replay_count", SINGLE_300_WINDOW, securityScopes(event), event);
            case "AUTH_HEADER_MISSING", "AUTH_CLIENT_ID_INVALID", "AUTH_CREDENTIAL_NOT_FOUND",
                 "AUTH_CREDENTIAL_INACTIVE", "AUTH_CREDENTIAL_EXPIRED", "AUTH_API_KEY_INVALID",
                 "AUTH_CLIENT_MISMATCH", "AUTH_TIMESTAMP_INVALID", "AUTH_TIMESTAMP_EXPIRED" ->
                    addMetric(increments, "auth_fail_count", SECURITY_FAST_WINDOWS, securityScopes(event), event);
            case PERMISSION_DENIED -> addMetric(increments, "permission_denied_count", SINGLE_300_WINDOW, securityScopes(event), event);
            case "ACCESS_POLICY_DENIED" -> addMetric(increments, "access_policy_denied_count", SINGLE_300_WINDOW, securityScopes(event), event);
            case "RATE_LIMIT_EXCEEDED" -> addMetric(increments, "rate_limit_exceeded_count", SINGLE_300_WINDOW, securityScopes(event), event);
            case "REQUEST_TOO_LARGE" -> addMetric(increments, "request_too_large_count", SINGLE_300_WINDOW, securityScopes(event), event);
            case "RESPONSE_TOO_LARGE" -> addMetric(increments, "response_too_large_count", SINGLE_300_WINDOW, securityScopes(event), event);
            case "TIMEOUT_EXCEEDED" -> addMetric(increments, TIMEOUT_COUNT, violationWindows(), failureScopes(event), event);
            case "RETRY_SCHEDULED" -> addMetric(increments, RETRY_COUNT, violationWindows(), failureScopes(event), event);
            case "RETRY_EXHAUSTED" -> addOutboundFailure(increments, "retry_exhausted_count", event);
            case "PUBLISH_FAILED" -> addOutboundFailure(increments, "publish_failed_count", event);
            case "PRODUCER_EXCEPTION" -> addOutboundFailure(increments, "producer_exception_count", event);
            case "BROKER_UNAVAILABLE" -> addOutboundFailure(increments, "broker_unavailable_count", event);
            case "SERIALIZATION_ERROR" -> addOutboundFailure(increments, "serialization_error_count", event);
            default -> {
            }
        }
    }

    private void addOutboundFailure(Set<MetricIncrement> increments, String metric, NormalizedEvent event) {
        addMetric(increments, metric, SINGLE_300_WINDOW, failureScopes(event), event);
        addMetric(increments, FAILED_COUNT, violationWindows(), failureScopes(event), event);
    }

    private void addDurationCounters(Set<MetricIncrement> increments, NormalizedEvent event) {
        if (event.durationMs() != null && event.latencyThresholdMs() != null && event.durationMs() > event.latencyThresholdMs()) {
            addMetric(increments, SLOW_REQUEST_COUNT, SINGLE_300_WINDOW, slowRequestScopes(event), event);
        }
    }

    private List<DistinctDeniedEndpointIncrement> buildDistinctDeniedEndpointIncrements(NormalizedEvent event) {
        if (!PERMISSION_DENIED.equals(event.resultCode()) || !StringUtils.hasText(event.endpointId())) {
            return List.of();
        }

        List<DistinctDeniedEndpointIncrement> increments = new ArrayList<>();
        for (Integer window : SINGLE_300_WINDOW) {
            long ttlSeconds = ttlSeconds(window);
            if (StringUtils.hasText(event.clientId())) {
                increments.add(new DistinctDeniedEndpointIncrement(window, "client", event.clientId(), event.endpointId(), event.timestamp(), ttlSeconds));
            }
            if (StringUtils.hasText(event.sourceIp())) {
                increments.add(new DistinctDeniedEndpointIncrement(window, "ip", event.sourceIp(), event.endpointId(), event.timestamp(), ttlSeconds));
            }
        }
        return List.copyOf(increments);
    }

    private void addMetric(Set<MetricIncrement> increments, String metric, List<Integer> windows, List<Scope> scopes, NormalizedEvent event) {
        for (Integer window : windows) {
            if (window == null || window <= 0) {
                continue;
            }
            long ttlSeconds = ttlSeconds(window);
            for (Scope scope : scopes) {
                increments.add(new MetricIncrement(metric, 1L, window, scope.scopeType(), scope.scopeKey(), event.timestamp(), ttlSeconds));
            }
        }
    }

    private List<Scope> successScopes(NormalizedEvent event) {
        List<Scope> scopes = new ArrayList<>();
        endpointScope(event, scopes);
        if (Boolean.TRUE.equals(properties.getSuccessClientScopeEnabled())) {
            endpointClientScope(event, scopes);
        }
        if (Boolean.TRUE.equals(properties.getSuccessIpScopeEnabled())) {
            endpointIpScope(event, scopes);
        }
        if (scopes.isEmpty()) {
            serviceScope(event, scopes);
            globalScope(event, scopes);
        }
        return scopes;
    }

    private List<Scope> nonSuccessScopes(NormalizedEvent event) {
        List<Scope> scopes = new ArrayList<>();
        endpointScope(event, scopes);
        endpointClientScope(event, scopes);
        endpointIpScope(event, scopes);
        serviceScope(event, scopes);
        globalScope(event, scopes);
        return scopes;
    }

    private List<Scope> securityScopes(NormalizedEvent event) {
        List<Scope> scopes = new ArrayList<>();
        endpointScope(event, scopes);
        endpointClientScope(event, scopes);
        endpointIpScope(event, scopes);
        return scopes;
    }

    private List<Scope> failureScopes(NormalizedEvent event) {
        List<Scope> scopes = new ArrayList<>();
        endpointScope(event, scopes);
        serviceScope(event, scopes);
        globalScope(event, scopes);
        return scopes;
    }

    private List<Scope> slowRequestScopes(NormalizedEvent event) {
        List<Scope> scopes = new ArrayList<>();
        endpointScope(event, scopes);
        endpointClientScope(event, scopes);
        if (Boolean.TRUE.equals(properties.getSuccessIpScopeEnabled())) {
            endpointIpScope(event, scopes);
        }
        if (scopes.isEmpty()) {
            serviceScope(event, scopes);
            globalScope(event, scopes);
        }
        return scopes;
    }

    private void endpointScope(NormalizedEvent event, List<Scope> scopes) {
        if (StringUtils.hasText(event.endpointId())) {
            scopes.add(new Scope(AnomalyScopeType.ENDPOINT, event.flowType() + ":" + event.endpointId()));
        }
    }

    private void endpointClientScope(NormalizedEvent event, List<Scope> scopes) {
        if (StringUtils.hasText(event.endpointId()) && StringUtils.hasText(event.clientId())) {
            scopes.add(new Scope(AnomalyScopeType.ENDPOINT_CLIENT, event.flowType() + ":" + event.endpointId() + ":client:" + event.clientId()));
        }
    }

    private void endpointIpScope(NormalizedEvent event, List<Scope> scopes) {
        if (StringUtils.hasText(event.endpointId()) && StringUtils.hasText(event.sourceIp())) {
            scopes.add(new Scope(AnomalyScopeType.ENDPOINT_IP, event.flowType() + ":" + event.endpointId() + ":ip:" + event.sourceIp()));
        }
    }

    private void serviceScope(NormalizedEvent event, List<Scope> scopes) {
        if (StringUtils.hasText(event.serviceName())) {
            scopes.add(new Scope(AnomalyScopeType.SERVICE, event.flowType() + ":service:" + event.serviceName()));
        }
    }

    private void globalScope(NormalizedEvent event, List<Scope> scopes) {
        scopes.add(new Scope(AnomalyScopeType.GLOBAL, event.flowType() + ":global"));
    }

    private NormalizedEvent normalize(SecurityLogEventMessage event) {
        return new NormalizedEvent(
                event.getTimestamp(),
                trim(event.getServiceName()),
                trim(event.getEndpointId()),
                uppercaseOrDefault(event.getFlowType(), "UNKNOWN"),
                StringUtils.hasText(event.getFlowType()),
                trim(event.getClientId()),
                trim(event.getSourceIp()),
                uppercaseOrDefault(event.getStatus(), "UNKNOWN"),
                uppercase(trim(event.getResultCode())),
                uppercase(trim(event.getErrorCode())),
                event.getDurationMs(),
                event.getLatencyThresholdMs(),
                event.getTimeoutMs(),
                event.getRetryAttempt());
    }

    private boolean hasAnyScopeAnchor(NormalizedEvent event) {
        return StringUtils.hasText(event.endpointId()) || StringUtils.hasText(event.serviceName()) || event.hasFlowType();
    }

    private boolean isDurationTimeout(NormalizedEvent event) {
        return event.durationMs() != null && event.timeoutMs() != null && event.durationMs() > event.timeoutMs();
    }

    private String metricForTimeout(NormalizedEvent event) {
        return "TIMEOUT".equals(event.status()) || "TIMEOUT_EXCEEDED".equals(event.resultCode()) ? TIMEOUT_COUNT : null;
    }

    private List<Integer> violationWindows() {
        return windows(properties.getViolationWindows(), List.of(60, 300, 900));
    }

    private List<Integer> windows(List<Integer> configured, List<Integer> defaults) {
        if (configured == null || configured.isEmpty()) {
            return defaults;
        }
        return configured.stream()
                .filter(window -> window != null && window > 0)
                .distinct()
                .toList();
    }

    private long ttlSeconds(int windowSeconds) {
        int multiplier = properties.getCounterTtlMultiplier() == null || properties.getCounterTtlMultiplier() <= 0
                ? 2
                : properties.getCounterTtlMultiplier();
        return (long) windowSeconds * multiplier;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String uppercase(String value) {
        return StringUtils.hasText(value) ? value.toUpperCase(Locale.ROOT) : null;
    }

    private String uppercaseOrDefault(String value, String defaultValue) {
        String normalized = uppercase(trim(value));
        return StringUtils.hasText(normalized) ? normalized : defaultValue;
    }

    private record Scope(AnomalyScopeType scopeType, String scopeKey) {
    }

    private record NormalizedEvent(
            Instant timestamp,
            String serviceName,
            String endpointId,
            String flowType,
            boolean hasFlowType,
            String clientId,
            String sourceIp,
            String status,
            String resultCode,
            String errorCode,
            Long durationMs,
            Long latencyThresholdMs,
            Long timeoutMs,
            Integer retryAttempt) {
        private boolean isSuccess() {
            return "SUCCESS".equals(status);
        }
    }
}
