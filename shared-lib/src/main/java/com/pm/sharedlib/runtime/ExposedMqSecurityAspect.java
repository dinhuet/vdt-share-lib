package com.pm.sharedlib.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.sharedlib.annotation.SharedApi;
import com.pm.sharedlib.config.VdtShareProperties;
import com.pm.sharedlib.endpoint.EndpointDefinition;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Aspect
@Slf4j
public class ExposedMqSecurityAspect {

    private static final int FORBIDDEN = 403;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int PAYLOAD_TOO_LARGE = 413;
    private static final String ACTIVE = "ACTIVE";
    private static final String MQ = "MQ";

    private final EndpointRegistry endpointRegistry;
    private final SecuritySettingsStore settingsStore;
    private final AccessPolicyEvaluator accessPolicyEvaluator;
    private final ClientAuthService clientAuthService;
    private final ClientPermissionChecker clientPermissionChecker;
    private final RateLimiter rateLimiter;
    private final VdtShareProperties properties;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ExposedMqSecurityAspect(
            EndpointRegistry endpointRegistry,
            SecuritySettingsStore settingsStore,
            AccessPolicyEvaluator accessPolicyEvaluator,
            ClientAuthService clientAuthService,
            ClientPermissionChecker clientPermissionChecker,
            RateLimiter rateLimiter,
            VdtShareProperties properties,
            ObjectMapper objectMapper,
            @Nullable KafkaTemplate<String, String> kafkaTemplate) {
        this.endpointRegistry = endpointRegistry;
        this.settingsStore = settingsStore;
        this.accessPolicyEvaluator = accessPolicyEvaluator;
        this.clientAuthService = clientAuthService;
        this.clientPermissionChecker = clientPermissionChecker;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Around("@annotation(sharedApi)")
    public Object aroundExposedMq(ProceedingJoinPoint joinPoint, SharedApi sharedApi) throws Throwable {
        if (!MQ.equalsIgnoreCase(sharedApi.protocol())) {
            return joinPoint.proceed();
        }

        var signature = (MethodSignature) joinPoint.getSignature();
        var method = signature.getMethod();
        var targetClass = ClassUtils.getUserClass(joinPoint.getTarget()).getName();
        var handlerMethod = method.getName();

        var topic = resolveTopic(sharedApi, method, joinPoint.getArgs());
        if (!StringUtils.hasText(topic)) {
            log.warn("MQ exposed API [{}] has no resolvable topic, skipping security check", sharedApi.name());
            return joinPoint.proceed();
        }

        var endpoint = endpointRegistry.findExposedMq(topic, targetClass, handlerMethod);
        if (endpoint.isEmpty()) {
            log.warn("MQ exposed API endpoint not found: topic={}, handler={}#{}", topic, targetClass, handlerMethod);
            return joinPoint.proceed();
        }

        var config = settingsStore.getExposedApi(endpoint.get().getEndpointId()).orElse(null);
        if (config == null) {
            log.warn("Exposed API runtime config not found for endpointId={}, skipping security check",
                    endpoint.get().getEndpointId());
            return joinPoint.proceed();
        }

        var kafkaHeaders = extractKafkaHeaders(joinPoint.getArgs());
        RuntimeAuthHeaders headers;
        byte[] payloadBytes = null;
        if (kafkaHeaders != null) {
            headers = new KafkaRuntimeAuthHeaders(kafkaHeaders);
            payloadBytes = extractPayloadBytes(joinPoint.getArgs());
        } else {
            log.warn("No Kafka ConsumerRecord or Message found in args of MQ exposed API [{}], skipping security check. "
                    + "Add ConsumerRecord or Message<?> parameter to listener method.", sharedApi.name());
            return joinPoint.proceed();
        }

        var responseTopic = headers.get(RuntimeSecurityHeaders.RESPONSE_TOPIC);
        var correlationId = headers.get(RuntimeSecurityHeaders.CORRELATION_ID);

        try {
            validateConfig(config);
            validateRequestSize(config, payloadBytes);

            var policies = settingsStore.getAccessPolicies(config.getId());
            var decision = accessPolicyEvaluator.evaluate(
                    policies,
                    "",
                    headers.get(RuntimeSecurityHeaders.CLIENT_ID));

            if (decision == AccessPolicyDecision.DENY) {
                throw new NonRetryableMqSecurityException(
                        FORBIDDEN, "ACCESS_POLICY_DENIED", "Request was denied by access policy");
            }

            AuthenticatedClient authenticatedClient = null;
            String rateLimitIdentityType = "anonymous";
            String rateLimitIdentityValue = "unknown";

            if (decision == AccessPolicyDecision.REQUIRE_AUTH) {
                authenticatedClient = clientAuthService.authenticate(headers);
                clientPermissionChecker.checkPermission(authenticatedClient.getClientId(), config.getId());
                rateLimitIdentityType = "client";
                rateLimitIdentityValue = authenticatedClient.getClientId().toString();
            } else if (StringUtils.hasText(headers.get(RuntimeSecurityHeaders.CLIENT_ID))) {
                rateLimitIdentityType = "client";
                rateLimitIdentityValue = headers.get(RuntimeSecurityHeaders.CLIENT_ID).trim();
            }

            checkRateLimit(config, rateLimitIdentityType, rateLimitIdentityValue);

            long start = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            if (config.getLatencyThresholdMs() != null && elapsed > config.getLatencyThresholdMs()) {
                log.warn("MQ exposed API [{}] topic {} latency threshold exceeded: {}ms > {}ms",
                        endpoint.get().getName(), topic, elapsed, config.getLatencyThresholdMs());
            }
            if (config.getTimeoutMs() != null && elapsed > config.getTimeoutMs()) {
                log.warn("MQ exposed API [{}] topic {} timed out: {}ms > {}ms",
                        endpoint.get().getName(), topic, elapsed, config.getTimeoutMs());
            }

            if (responseTopic != null && result != null && kafkaTemplate != null) {
                publishResponse(responseTopic, correlationId, endpoint.get(), config, result, null);
            }

            return result;

        } catch (NonRetryableMqSecurityException | RetryableMqSecurityException e) {
            if (responseTopic != null && kafkaTemplate != null) {
                publishResponse(responseTopic, correlationId, endpoint.get(), config, null, e);
                return null;
            }
            throw e;
        } catch (RuntimeSecurityException e) {
            if (responseTopic != null && kafkaTemplate != null) {
                publishResponse(responseTopic, correlationId, endpoint.get(), config, null, e);
                return null;
            }
            throw new NonRetryableMqSecurityException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private String resolveTopic(SharedApi sharedApi, java.lang.reflect.Method method, Object[] args) {
        if (!sharedApi.topic().isBlank()) {
            return sharedApi.topic();
        }
        for (var arg : args) {
            if (arg instanceof ConsumerRecord<?, ?> record) {
                return record.topic();
            }
            if (arg instanceof Message<?> message) {
                var nativeHeaders = message.getHeaders().get("kafka_nativeHeaders");
                if (nativeHeaders != null) {
                    return message.getHeaders().get("kafka_receivedTopic", String.class);
                }
            }
        }
        var kafkaListener = method.getAnnotation(org.springframework.kafka.annotation.KafkaListener.class);
        if (kafkaListener != null && kafkaListener.topics().length > 0) {
            return kafkaListener.topics()[0];
        }
        return "";
    }

    private org.apache.kafka.common.header.Headers extractKafkaHeaders(Object[] args) {
        for (var arg : args) {
            if (arg instanceof ConsumerRecord<?, ?> record) {
                return record.headers();
            }
            if (arg instanceof Message<?> message) {
                var nativeHeaders = message.getHeaders().get("kafka_nativeHeaders",
                        org.springframework.kafka.support.KafkaMessageHeaderAccessor.class);
                if (nativeHeaders != null) {
                    return nativeHeaders.toMessageHeaders().get("kafka_nativeHeaders",
                            org.apache.kafka.common.header.Headers.class);
                }
            }
        }
        return null;
    }

    private byte[] extractPayloadBytes(Object[] args) {
        for (var arg : args) {
            if (arg instanceof ConsumerRecord<?, ?> record) {
                if (record.value() instanceof String str) {
                    return str.getBytes(StandardCharsets.UTF_8);
                }
                if (record.value() instanceof byte[] bytes) {
                    return bytes;
                }
                if (record.value() != null) {
                    return record.value().toString().getBytes(StandardCharsets.UTF_8);
                }
                return null;
            }
            if (arg instanceof Message<?> message) {
                var payload = message.getPayload();
                if (payload instanceof String str) {
                    return str.getBytes(StandardCharsets.UTF_8);
                }
                if (payload instanceof byte[] bytes) {
                    return bytes;
                }
                if (payload != null) {
                    return payload.toString().getBytes(StandardCharsets.UTF_8);
                }
                return null;
            }
        }
        return null;
    }

    private void validateConfig(ExposedApiRuntimeConfig config) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new NonRetryableMqSecurityException(FORBIDDEN, "EXPOSED_API_DISABLED", "Exposed API is disabled");
        }
        if (!ACTIVE.equalsIgnoreCase(config.getSyncStatus())) {
            throw new NonRetryableMqSecurityException(FORBIDDEN, "EXPOSED_API_NOT_ACTIVE", "Exposed API is not active");
        }
        if (!MQ.equalsIgnoreCase(config.getProtocol())) {
            throw new NonRetryableMqSecurityException(FORBIDDEN, "EXPOSED_API_PROTOCOL_MISMATCH", "Exposed API protocol is not MQ");
        }
    }

    private void validateRequestSize(ExposedApiRuntimeConfig config, byte[] payloadBytes) {
        if (config.getMaxRequestKb() == null || config.getMaxRequestKb() <= 0) {
            return;
        }
        if (payloadBytes == null) {
            return;
        }
        long maxBytes = config.getMaxRequestKb() * 1024L;
        if (payloadBytes.length > maxBytes) {
            throw new NonRetryableMqSecurityException(
                    PAYLOAD_TOO_LARGE, "REQUEST_TOO_LARGE", "Message payload is too large");
        }
    }

    private void checkRateLimit(ExposedApiRuntimeConfig config, String identityType, String identityValue) {
        var result = rateLimiter.check(config, identityType, identityValue);
        if (!result.allowed()) {
            throw new NonRetryableMqSecurityException(
                    TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED",
                    "Rate limit exceeded: " + result.currentRequests() + "/" + result.maxRequests()
                            + " requests in " + result.windowSeconds() + " seconds");
        }
    }

    private void publishResponse(String responseTopic, String correlationId,
                                  EndpointDefinition endpoint, ExposedApiRuntimeConfig config,
                                  Object result, RuntimeSecurityException error) {
        try {
            var envelope = buildEnvelope(correlationId, endpoint, config, result, error);
            var json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(responseTopic, endpoint.getEndpointId().toString(), json);
        } catch (Exception e) {
            log.warn("Failed to publish MQ security response to topic={}", responseTopic, e);
        }
    }

    private Map<String, Object> buildEnvelope(String correlationId, EndpointDefinition endpoint,
                                                ExposedApiRuntimeConfig config,
                                                Object result, RuntimeSecurityException error) {
        if (error != null) {
            return Map.of(
                    "success", false,
                    "status", error.getStatusCode(),
                    "code", error.getErrorCode(),
                    "message", error.getMessage(),
                    "correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString(),
                    "endpointId", endpoint.getEndpointId().toString(),
                    "data", null
            );
        }
        return Map.of(
                "success", true,
                "status", 200,
                "code", "OK",
                "message", "Success",
                "correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString(),
                "endpointId", endpoint.getEndpointId().toString(),
                "data", result
        );
    }
}
