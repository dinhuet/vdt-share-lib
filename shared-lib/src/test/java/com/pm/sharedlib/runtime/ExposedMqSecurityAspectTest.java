package com.pm.sharedlib.runtime;

import com.pm.sharedlib.annotation.SharedApi;
import com.pm.sharedlib.config.VdtShareProperties;
import com.pm.sharedlib.endpoint.EndpointDefinition;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import com.pm.sharedlib.endpoint.EndpointType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExposedMqSecurityAspectTest {

    static final UUID ENDPOINT_ID = UUID.randomUUID();
    static final UUID EXPOSED_API_DB_ID = UUID.randomUUID();
    static final String TOPIC = "test.topic";

    @Mock EndpointRegistry endpointRegistry;
    @Mock SecuritySettingsStore settingsStore;
    @Mock AccessPolicyEvaluator accessPolicyEvaluator;
    @Mock ClientAuthService clientAuthService;
    @Mock ClientPermissionChecker clientPermissionChecker;
    @Mock RateLimiter rateLimiter;
    @Mock com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    @Mock ProceedingJoinPoint joinPoint;
    @Mock MethodSignature signature;

    ExposedMqSecurityAspect aspect;
    VdtShareProperties properties;
    ExposedApiRuntimeConfig config;
    EndpointDefinition endpoint;
    SharedApi sharedApi;
    Headers kafkaHeaders;

    @BeforeEach
    void setUp() {
        properties = new VdtShareProperties();
        aspect = new ExposedMqSecurityAspect(
                endpointRegistry, settingsStore, accessPolicyEvaluator,
                clientAuthService, clientPermissionChecker, rateLimiter,
                properties, objectMapper, kafkaTemplate);

        endpoint = EndpointDefinition.builder()
                .endpointId(ENDPOINT_ID)
                .endpointKey("EXPOSED:MQ:" + TOPIC + ":com.pm.TestListener#onMessage")
                .type(EndpointType.EXPOSED)
                .protocol("MQ")
                .name("test-api")
                .topic(TOPIC)
                .handlerClass("com.pm.TestListener")
                .handlerMethod("onMessage")
                .build();

        config = ExposedApiRuntimeConfig.builder()
                .id(EXPOSED_API_DB_ID)
                .endpointId(ENDPOINT_ID)
                .protocol("MQ")
                .enabled(true)
                .syncStatus("ACTIVE")
                .maxRequestKb(10)
                .build();

        sharedApi = createSharedApiMock("MQ", TOPIC);
    }

    @Test
    void shouldSkipWhenProtocolIsNotMq() throws Throwable {
        var httpSharedApi = createSharedApiMock("HTTP", "");

        aspect.aroundExposedMq(joinPoint, httpSharedApi);

        verify(joinPoint).proceed();
    }

    @Test
    void shouldSkipSecurityWhenMqEndpointNotFound() throws Throwable {
        stubJoinPointDefaults();
        when(endpointRegistry.findExposedMq(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        aspect.aroundExposedMq(joinPoint, sharedApi);

        verify(joinPoint).proceed();
    }

    @Test
    void shouldThrowWhenExposedApiIsDisabled() throws Throwable {
        config.setEnabled(false);
        setupDefaultMocks();

        assertThatThrownBy(() -> aspect.aroundExposedMq(joinPoint, sharedApi))
                .isInstanceOf(RuntimeSecurityException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void shouldThrowWhenSyncStatusIsNotActive() throws Throwable {
        config.setSyncStatus("STALE");
        setupDefaultMocks();

        assertThatThrownBy(() -> aspect.aroundExposedMq(joinPoint, sharedApi))
                .isInstanceOf(RuntimeSecurityException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void shouldThrowWhenProtocolIsNotMq() throws Throwable {
        config.setProtocol("HTTP");
        setupDefaultMocks();

        assertThatThrownBy(() -> aspect.aroundExposedMq(joinPoint, sharedApi))
                .isInstanceOf(RuntimeSecurityException.class)
                .hasMessageContaining("protocol");
    }

    @Test
    void shouldThrowWhenAccessPolicyDenies() throws Throwable {
        setupAuthMocks();

        assertThatThrownBy(() -> aspect.aroundExposedMq(joinPoint, sharedApi))
                .isInstanceOf(RuntimeSecurityException.class)
                .hasMessageContaining("denied");
    }

    @Test
    void shouldProceedWhenWhitelistTrusted() throws Throwable {
        setupTrustedMocks();

        aspect.aroundExposedMq(joinPoint, sharedApi);

        verify(joinPoint).proceed();
    }

    @Test
    void shouldProceedWhenAuthSucceeds() throws Throwable {
        setupFullAuthMocks();

        aspect.aroundExposedMq(joinPoint, sharedApi);

        verify(joinPoint).proceed();
    }

    @Test
    void shouldThrowWhenRateLimitExceeded() throws Throwable {
        setupFullAuthMocks();
        lenient().when(rateLimiter.check(any(), anyString(), anyString()))
                .thenReturn(new RateLimitResult(false, 101, 100, 60));

        assertThatThrownBy(() -> aspect.aroundExposedMq(joinPoint, sharedApi))
                .isInstanceOf(RuntimeSecurityException.class)
                .hasMessageContaining("Rate limit exceeded");
    }

    void stubJoinPointDefaults() {
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getMethod()).thenReturn(getClass().getDeclaredMethods()[0]);
        lenient().when(joinPoint.getTarget()).thenReturn(new Object());
    }

    void setupDefaultMocks() {
        stubJoinPointDefaults();
        lenient().when(endpointRegistry.findExposedMq(eq(TOPIC), anyString(), anyString()))
                .thenReturn(Optional.of(endpoint));
        lenient().when(settingsStore.getExposedApi(ENDPOINT_ID)).thenReturn(Optional.of(config));
        kafkaHeaders = createKafkaHeaders();
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{createConsumerRecord()});
    }

    void setupAuthMocks() {
        setupDefaultMocks();
        lenient().when(accessPolicyEvaluator.evaluate(any(), anyString(), anyString()))
                .thenReturn(AccessPolicyDecision.DENY);
        lenient().when(settingsStore.getAccessPolicies(EXPOSED_API_DB_ID)).thenReturn(java.util.List.of());
    }

    void setupTrustedMocks() {
        setupAuthMocks();
        lenient().when(accessPolicyEvaluator.evaluate(any(), anyString(), anyString()))
                .thenReturn(AccessPolicyDecision.ALLOW_TRUSTED);
        lenient().when(rateLimiter.check(any(), anyString(), anyString()))
                .thenReturn(new RateLimitResult(true, 1, 100, 60));
    }

    void setupFullAuthMocks() {
        setupDefaultMocks();
        lenient().when(settingsStore.getAccessPolicies(EXPOSED_API_DB_ID)).thenReturn(java.util.List.of());
        lenient().when(accessPolicyEvaluator.evaluate(any(), anyString(), anyString()))
                .thenReturn(AccessPolicyDecision.REQUIRE_AUTH);

        var clientId = UUID.randomUUID();
        lenient().when(clientAuthService.authenticate(any(RuntimeAuthHeaders.class)))
                .thenReturn(new AuthenticatedClient(clientId, "code", "name", "key-id"));
        lenient().when(rateLimiter.check(any(), eq("client"), eq(clientId.toString())))
                .thenReturn(new RateLimitResult(true, 1, 100, 60));
    }

    SharedApi createSharedApiMock(String protocol, String topic) {
        var mock = org.mockito.Mockito.mock(SharedApi.class);
        lenient().when(mock.protocol()).thenReturn(protocol);
        lenient().when(mock.topic()).thenReturn(topic);
        lenient().when(mock.name()).thenReturn("test-api");
        return mock;
    }

    Headers createKafkaHeaders() {
        var headers = new RecordHeaders();
        headers.add("X-Client-Id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        headers.add("X-Key-Id", "key-id".getBytes(StandardCharsets.UTF_8));
        headers.add("X-Api-Key", "api-key".getBytes(StandardCharsets.UTF_8));
        return headers;
    }

    ConsumerRecord<String, String> createConsumerRecord() {
        return new ConsumerRecord<String, String>(
                TOPIC,
                0,
                0L,
                System.currentTimeMillis(),
                TimestampType.CREATE_TIME,
                0,
                -1,
                null,
                "test-payload",
                kafkaHeaders,
                Optional.empty()
        );
    }
}
