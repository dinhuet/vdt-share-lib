package com.pm.sharedlib.runtime;

import com.pm.sharedlib.endpoint.EndpointDefinition;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
public class ExposedMqSecurityInterceptor implements RecordInterceptor<Object, Object> {

    private static final int FORBIDDEN = 403;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int PAYLOAD_TOO_LARGE = 413;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final String ACTIVE = "ACTIVE";
    private static final String MQ = "MQ";
    private static final String ANONYMOUS = "anonymous";
    private static final String UNKNOWN = "unknown";

    private final ThreadLocal<SecurityTimingContext> timingContext = new ThreadLocal<>();

    private final EndpointRegistry endpointRegistry;
    private final SecuritySettingsStore settingsStore;
    private final AccessPolicyEvaluator accessPolicyEvaluator;
    private final ClientAuthService clientAuthService;
    private final ClientPermissionChecker clientPermissionChecker;
    private final RateLimiter rateLimiter;
    private final SecurityAuditLogger auditLogger;

    public ExposedMqSecurityInterceptor(
            EndpointRegistry endpointRegistry,
            SecuritySettingsStore settingsStore,
            AccessPolicyEvaluator accessPolicyEvaluator,
            ClientAuthService clientAuthService,
            ClientPermissionChecker clientPermissionChecker,
            RateLimiter rateLimiter) {
        this(endpointRegistry, settingsStore, accessPolicyEvaluator, clientAuthService, clientPermissionChecker, rateLimiter, null);
    }

    public ExposedMqSecurityInterceptor(
            EndpointRegistry endpointRegistry,
            SecuritySettingsStore settingsStore,
            AccessPolicyEvaluator accessPolicyEvaluator,
            ClientAuthService clientAuthService,
            ClientPermissionChecker clientPermissionChecker,
            RateLimiter rateLimiter,
            SecurityAuditLogger auditLogger) {
        this.endpointRegistry = endpointRegistry;
        this.settingsStore = settingsStore;
        this.accessPolicyEvaluator = accessPolicyEvaluator;
        this.clientAuthService = clientAuthService;
        this.clientPermissionChecker = clientPermissionChecker;
        this.rateLimiter = rateLimiter;
        this.auditLogger = auditLogger;
    }

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        timingContext.remove();
        var topic = record.topic();
        var endpoint = endpointRegistry.findExposedMqByTopic(topic).orElse(null);
        if (endpoint == null) {
            log.debug("No exposed MQ endpoint registered for Kafka topic {}, passing record through", topic);
            return record;
        }

        var startedAt = System.currentTimeMillis();
        ExposedApiRuntimeConfig auditConfig = null;
        String clientId = null;
        RateLimitResult rateLimitResult = null;
        try {
            var config = settingsStore.getExposedApi(endpoint.getEndpointId())
                    .orElseThrow(() -> nonRetryable(
                            FORBIDDEN,
                            "EXPOSED_API_CONFIG_MISSING",
                            "Exposed API runtime config was not found"));
            auditConfig = config;
            validateConfig(config);
            validateRequestSize(config, record);

            var headers = new KafkaRuntimeAuthHeaders(record.headers());
            var policies = settingsStore.getAccessPolicies(config.getId());
            var decision = accessPolicyEvaluator.evaluate(
                    policies,
                    "",
                    headers.get(RuntimeSecurityHeaders.CLIENT_ID));

            if (decision == AccessPolicyDecision.DENY) {
                throw nonRetryable(FORBIDDEN, "ACCESS_POLICY_DENIED", "Request was denied by access policy");
            }

            if (decision == AccessPolicyDecision.REQUIRE_AUTH) {
                var payloadBytes = extractPayloadBytes(record);
                var client = clientAuthService.authenticate(headers, topic, payloadBytes);
                clientPermissionChecker.checkPermission(client.getClientId(), config.getId());
                clientId = client.getClientId().toString();
                rateLimitResult = checkRateLimit(config, "client", clientId);
            } else {
                var clientIdHeader = headers.get(RuntimeSecurityHeaders.CLIENT_ID);
                clientId = StringUtils.hasText(clientIdHeader) ? clientIdHeader.trim() : null;
                rateLimitResult = checkRateLimit(
                        config,
                        StringUtils.hasText(clientIdHeader) ? "client" : ANONYMOUS,
                        StringUtils.hasText(clientIdHeader) ? clientIdHeader.trim() : UNKNOWN);
            }

            timingContext.set(new SecurityTimingContext(startedAt, config, endpoint, topic, clientId, rateLimitResult));
            return record;
        } catch (NonRetryableMqSecurityException | RetryableMqSecurityException e) {
            audit(endpoint, auditConfig, topic, clientId, "DENIED", e.getErrorCode(), e.getErrorCode(), e.getMessage(),
                    System.currentTimeMillis() - startedAt, rateLimitResult);
            timingContext.remove();
            throw e;
        } catch (RuntimeSecurityException e) {
            audit(endpoint, auditConfig, topic, clientId, "DENIED", e.getErrorCode(), e.getErrorCode(), e.getMessage(),
                    System.currentTimeMillis() - startedAt, rateLimitResult);
            timingContext.remove();
            throw nonRetryable(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RuntimeException e) {
            timingContext.remove();
            log.warn("MQ runtime security check failed for topic {}", topic, e);
            audit(endpoint, auditConfig, topic, clientId, "FAILED", "RUNTIME_SECURITY_UNAVAILABLE", "RUNTIME_SECURITY_UNAVAILABLE",
                    e.getMessage(), System.currentTimeMillis() - startedAt, rateLimitResult);
            throw nonRetryable(SERVICE_UNAVAILABLE, "RUNTIME_SECURITY_UNAVAILABLE", "Runtime security check failed");
        }
    }

    @Override
    public void success(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        try {
            logTimingIfPresent();
        } finally {
            timingContext.remove();
        }
    }

    @Override
    public void failure(ConsumerRecord<Object, Object> record, Exception exception, Consumer<Object, Object> consumer) {
        try {
            log.debug("MQ security completed with listener failure for topic {}", record.topic(), exception);
            var context = timingContext.get();
            if (context != null) {
                audit(context.endpoint(), context.config(), context.topic(), context.clientId(), "FAILED", "LISTENER_FAILED",
                        exception == null ? null : exception.getClass().getSimpleName(), exception == null ? null : exception.getMessage(),
                        System.currentTimeMillis() - context.startTimeMillis(), context.rateLimitResult());
            }
        } finally {
            timingContext.remove();
        }
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        timingContext.remove();
    }

    private void validateConfig(ExposedApiRuntimeConfig config) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw nonRetryable(FORBIDDEN, "EXPOSED_API_DISABLED", "Exposed API is disabled");
        }
        if (!ACTIVE.equalsIgnoreCase(config.getSyncStatus())) {
            throw nonRetryable(FORBIDDEN, "EXPOSED_API_NOT_ACTIVE", "Exposed API is not active");
        }
        if (!MQ.equalsIgnoreCase(config.getProtocol())) {
            throw nonRetryable(FORBIDDEN, "EXPOSED_API_PROTOCOL_MISMATCH", "Exposed API protocol is not MQ");
        }
    }

    private void validateRequestSize(ExposedApiRuntimeConfig config, ConsumerRecord<Object, Object> record) {
        if (config.getMaxRequestKb() == null || config.getMaxRequestKb() <= 0) {
            return;
        }
        var size = record.serializedValueSize() >= 0 ? record.serializedValueSize() : payloadSize(record.value());
        if (size < 0) {
            return;
        }
        long maxBytes = config.getMaxRequestKb() * 1024L;
        if (size > maxBytes) {
            throw nonRetryable(PAYLOAD_TOO_LARGE, "REQUEST_TOO_LARGE", "Message payload is too large");
        }
    }

    private int payloadSize(Object value) {
        if (value instanceof byte[] bytes) {
            return bytes.length;
        }
        if (value instanceof String str) {
            return str.getBytes(StandardCharsets.UTF_8).length;
        }
        return -1;
    }

    private byte[] extractPayloadBytes(ConsumerRecord<Object, Object> record) {
        if (record.value() instanceof byte[] bytes) {
            return bytes;
        }
        if (record.value() instanceof String str) {
            return str.getBytes(StandardCharsets.UTF_8);
        }
        if (record.value() == null) {
            return new byte[0];
        }
        return null;
    }

    private RateLimitResult checkRateLimit(ExposedApiRuntimeConfig config, String identityType, String identityValue) {
        var result = rateLimiter.check(config, identityType, identityValue);
        if (!result.allowed()) {
            throw nonRetryable(
                    TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED",
                    "Rate limit exceeded: " + result.currentRequests() + "/" + result.maxRequests()
                            + " requests in " + result.windowSeconds() + " seconds");
        }
        return result;
    }

    private void logTimingIfPresent() {
        var context = timingContext.get();
        if (context == null) {
            return;
        }
        var elapsed = System.currentTimeMillis() - context.startTimeMillis();
        var config = context.config();
        if (config.getLatencyThresholdMs() != null && elapsed > config.getLatencyThresholdMs()) {
            log.warn("MQ exposed API [{}] topic {} latency threshold exceeded: {}ms > {}ms",
                    context.endpoint().getName(), context.topic(), elapsed, config.getLatencyThresholdMs());
        }
        if (config.getTimeoutMs() != null && elapsed > config.getTimeoutMs()) {
            log.warn("MQ exposed API [{}] topic {} timed out: {}ms > {}ms",
                    context.endpoint().getName(), context.topic(), elapsed, config.getTimeoutMs());
        }
        audit(context.endpoint(), config, context.topic(), context.clientId(), "SUCCESS", "SUCCESS", null, null,
                elapsed, context.rateLimitResult());
    }

    private void audit(EndpointDefinition endpoint,
                       ExposedApiRuntimeConfig config,
                       String topic,
                       String clientId,
                       String status,
                       String resultCode,
                       String errorCode,
                       String denyReason,
                       long durationMs,
                       RateLimitResult rateLimitResult) {
        if (auditLogger == null) {
            return;
        }
        try {
            var retentionDays = config == null ? null : config.getLogRetentionDays();
            auditLogger.log(SecurityLogEvent.builder()
                    .timestamp(Instant.now())
                    .serviceName(config == null ? null : config.getServiceName())
                    .endpointId(endpoint.getEndpointId())
                    .endpointName(config != null && StringUtils.hasText(config.getApiName()) ? config.getApiName() : endpoint.getName())
                    .flowType("INBOUND_MQ")
                    .direction("INBOUND")
                    .protocol("MQ")
                    .topic(topic)
                    .clientId(clientId)
                    .status(status)
                    .resultCode(resultCode)
                    .errorCode(errorCode)
                    .denyReason(denyReason)
                    .durationMs(durationMs)
                    .latencyThresholdMs(config == null ? null : config.getLatencyThresholdMs())
                    .timeoutMs(config == null ? null : config.getTimeoutMs())
                    .rateLimitCurrent(rateLimitResult == null ? null : rateLimitResult.currentRequests())
                    .rateLimitMax(rateLimitResult == null ? null : rateLimitResult.maxRequests())
                    .rateLimitWindowSec(rateLimitResult == null ? null : Math.toIntExact(rateLimitResult.windowSeconds()))
                    .retentionDays(SecurityLogRetentionBucketMapper.normalizedDays(retentionDays))
                    .retentionBucket(SecurityLogRetentionBucketMapper.bucket(retentionDays))
                    .build());
        } catch (RuntimeException e) {
            log.warn("security_audit_emit_failed flowType=INBOUND_MQ endpointId={}", endpoint.getEndpointId(), e);
        }
    }

    private NonRetryableMqSecurityException nonRetryable(int statusCode, String errorCode, String message) {
        return new NonRetryableMqSecurityException(statusCode, errorCode, message);
    }

    private record SecurityTimingContext(
            long startTimeMillis,
            ExposedApiRuntimeConfig config,
            EndpointDefinition endpoint,
            String topic,
            String clientId,
            RateLimitResult rateLimitResult) {
    }
}
