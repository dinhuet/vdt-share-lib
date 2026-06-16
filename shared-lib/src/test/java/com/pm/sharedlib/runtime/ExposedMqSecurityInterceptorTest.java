package com.pm.sharedlib.runtime;

import com.pm.sharedlib.endpoint.EndpointDefinition;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import com.pm.sharedlib.endpoint.EndpointType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExposedMqSecurityInterceptorTest {

    private static final UUID ENDPOINT_ID = UUID.randomUUID();
    private static final UUID CONFIG_ID = UUID.randomUUID();
    private static final String TOPIC = "orders.created";

    @Mock EndpointRegistry endpointRegistry;
    @Mock SecuritySettingsStore settingsStore;
    @Mock AccessPolicyEvaluator accessPolicyEvaluator;
    @Mock ClientAuthService clientAuthService;
    @Mock ClientPermissionChecker clientPermissionChecker;
    @Mock RateLimiter rateLimiter;
    @Mock SecurityAuditLogger auditLogger;

    ExposedMqSecurityInterceptor interceptor;
    EndpointDefinition endpoint;
    ExposedApiRuntimeConfig config;

    @BeforeEach
    void setUp() {
        interceptor = new ExposedMqSecurityInterceptor(
                endpointRegistry,
                settingsStore,
                accessPolicyEvaluator,
                clientAuthService,
                clientPermissionChecker,
                rateLimiter,
                auditLogger);
        endpoint = EndpointDefinition.builder()
                .endpointId(ENDPOINT_ID)
                .type(EndpointType.EXPOSED)
                .protocol("MQ")
                .name("orders-created")
                .topic(TOPIC)
                .build();
        config = ExposedApiRuntimeConfig.builder()
                .id(CONFIG_ID)
                .endpointId(ENDPOINT_ID)
                .protocol("MQ")
                .enabled(true)
                .syncStatus("ACTIVE")
                .build();
    }

    @Test
    void intercept_shouldPassThroughWhenEndpointMissing() {
        var record = record("payload");
        when(endpointRegistry.findExposedMqByTopic(TOPIC)).thenReturn(Optional.empty());

        var result = interceptor.intercept(record, null);

        assertThat(result).isSameAs(record);
        verify(settingsStore, never()).getExposedApi(any());
    }

    @Test
    void intercept_shouldFailClosedWhenRuntimeConfigMissing() {
        when(endpointRegistry.findExposedMqByTopic(TOPIC)).thenReturn(Optional.of(endpoint));
        when(settingsStore.getExposedApi(ENDPOINT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.intercept(record("payload"), null))
                .isInstanceOf(NonRetryableMqSecurityException.class)
                .hasMessageContaining("runtime config");
    }

    @Test
    void intercept_shouldRejectDisabledInactiveAndProtocolMismatch() {
        assertConfigRejected(ExposedApiRuntimeConfig.builder()
                .id(CONFIG_ID).endpointId(ENDPOINT_ID).protocol("MQ").enabled(false).syncStatus("ACTIVE").build(), "disabled");
        assertConfigRejected(ExposedApiRuntimeConfig.builder()
                .id(CONFIG_ID).endpointId(ENDPOINT_ID).protocol("MQ").enabled(true).syncStatus("STALE").build(), "not active");
        assertConfigRejected(ExposedApiRuntimeConfig.builder()
                .id(CONFIG_ID).endpointId(ENDPOINT_ID).protocol("HTTP").enabled(true).syncStatus("ACTIVE").build(), "protocol");
    }

    @Test
    void intercept_shouldRejectAccessPolicyDeny() {
        setupConfig();
        when(accessPolicyEvaluator.evaluate(any(), eq(""), any())).thenReturn(AccessPolicyDecision.DENY);

        assertThatThrownBy(() -> interceptor.intercept(record("payload"), null))
                .isInstanceOf(NonRetryableMqSecurityException.class)
                .hasMessageContaining("denied");
    }

    @Test
    void intercept_shouldSkipAuthForTrustedClientAndRateLimitByHeaderClient() {
        var clientId = UUID.randomUUID().toString();
        setupConfig();
        when(accessPolicyEvaluator.evaluate(any(), eq(""), eq(clientId))).thenReturn(AccessPolicyDecision.ALLOW_TRUSTED);
        when(rateLimiter.check(config, "client", clientId)).thenReturn(new RateLimitResult(true, 1, 10, 60));

        interceptor.intercept(record("payload", clientId), null);

        verify(clientAuthService, never()).authenticate(any(RuntimeAuthHeaders.class), anyString(), any());
        verify(rateLimiter).check(config, "client", clientId);
    }

    @Test
    void intercept_shouldAuthenticateCheckPermissionAndRateLimitAuthenticatedClient() {
        var clientId = UUID.randomUUID();
        setupConfig();
        when(accessPolicyEvaluator.evaluate(any(), eq(""), any())).thenReturn(AccessPolicyDecision.REQUIRE_AUTH);
        when(clientAuthService.authenticate(any(RuntimeAuthHeaders.class), eq(TOPIC), eq("payload".getBytes(StandardCharsets.UTF_8))))
                .thenReturn(new AuthenticatedClient(clientId, "code", "name", "key-id"));
        when(rateLimiter.check(config, "client", clientId.toString())).thenReturn(new RateLimitResult(true, 1, 10, 60));

        interceptor.intercept(record("payload"), null);

        verify(clientPermissionChecker).checkPermission(clientId, CONFIG_ID);
        verify(rateLimiter).check(config, "client", clientId.toString());
    }

    @Test
    void success_shouldAuditInboundMqSuccess() {
        setupTrustedPass();
        var record = record("payload");

        interceptor.intercept(record, null);
        interceptor.success(record, null);

        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(event ->
                "INBOUND_MQ".equals(event.getFlowType()) && "SUCCESS".equals(event.getStatus()) && TOPIC.equals(event.getTopic())));
    }

    @Test
    void intercept_shouldWrapMissingAuthHeaderAsNonRetryable() {
        setupConfig();
        when(accessPolicyEvaluator.evaluate(any(), eq(""), any())).thenReturn(AccessPolicyDecision.REQUIRE_AUTH);
        when(clientAuthService.authenticate(any(RuntimeAuthHeaders.class), eq(TOPIC), any()))
                .thenThrow(new RuntimeSecurityException(401, RuntimeSecurityErrorCodes.AUTH_HEADER_MISSING, "Missing required header"));

        assertThatThrownBy(() -> interceptor.intercept(record("payload"), null))
                .isInstanceOf(NonRetryableMqSecurityException.class)
                .hasMessageContaining("Missing required header");
    }

    @Test
    void intercept_shouldRejectRateLimitExceeded() {
        var clientId = UUID.randomUUID();
        setupConfig();
        when(accessPolicyEvaluator.evaluate(any(), eq(""), any())).thenReturn(AccessPolicyDecision.REQUIRE_AUTH);
        when(clientAuthService.authenticate(any(RuntimeAuthHeaders.class), eq(TOPIC), any()))
                .thenReturn(new AuthenticatedClient(clientId, "code", "name", "key-id"));
        when(rateLimiter.check(config, "client", clientId.toString())).thenReturn(new RateLimitResult(false, 2, 1, 60));

        assertThatThrownBy(() -> interceptor.intercept(record("payload"), null))
                .isInstanceOf(NonRetryableMqSecurityException.class)
                .hasMessageContaining("Rate limit exceeded");
        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(event ->
                "INBOUND_MQ".equals(event.getFlowType()) && "RATE_LIMIT_EXCEEDED".equals(event.getErrorCode())));
    }

    @Test
    void intercept_shouldRejectOversizedStringPayload() {
        config.setMaxRequestKb(1);
        setupConfig();

        assertThatThrownBy(() -> interceptor.intercept(record("x".repeat(1025)), null))
                .isInstanceOf(NonRetryableMqSecurityException.class)
                .hasMessageContaining("too large");
    }

    @Test
    void callbacks_shouldClearTimingAfterSuccessFailureAndAfterRecord() {
        setupTrustedPass();
        var record = record("payload");

        interceptor.intercept(record, null);
        interceptor.success(record, null);
        interceptor.failure(record, new RuntimeException("listener failed"), null);
        interceptor.afterRecord(record, null);
    }

    private void assertConfigRejected(ExposedApiRuntimeConfig rejectedConfig, String message) {
        when(endpointRegistry.findExposedMqByTopic(TOPIC)).thenReturn(Optional.of(endpoint));
        when(settingsStore.getExposedApi(ENDPOINT_ID)).thenReturn(Optional.of(rejectedConfig));

        assertThatThrownBy(() -> interceptor.intercept(record("payload"), null))
                .isInstanceOf(NonRetryableMqSecurityException.class)
                .hasMessageContaining(message);
    }

    private void setupTrustedPass() {
        setupConfig();
        when(accessPolicyEvaluator.evaluate(any(), eq(""), any())).thenReturn(AccessPolicyDecision.ALLOW_TRUSTED);
        when(rateLimiter.check(any(), anyString(), anyString())).thenReturn(new RateLimitResult(true, 1, 10, 60));
    }

    private void setupConfig() {
        lenient().when(endpointRegistry.findExposedMqByTopic(TOPIC)).thenReturn(Optional.of(endpoint));
        lenient().when(settingsStore.getExposedApi(ENDPOINT_ID)).thenReturn(Optional.of(config));
        lenient().when(settingsStore.getAccessPolicies(CONFIG_ID)).thenReturn(List.of());
    }

    private ConsumerRecord<Object, Object> record(String payload) {
        return record(payload, UUID.randomUUID().toString());
    }

    private ConsumerRecord<Object, Object> record(String payload, String clientId) {
        var record = new ConsumerRecord<Object, Object>(TOPIC, 0, 0L, null, payload);
        record.headers().add(RuntimeSecurityHeaders.CLIENT_ID, clientId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(RuntimeSecurityHeaders.KEY_ID, "key-id".getBytes(StandardCharsets.UTF_8));
        record.headers().add(RuntimeSecurityHeaders.API_KEY, "api-key".getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
