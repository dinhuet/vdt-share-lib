package com.pm.sharedlib.runtime;

import com.pm.sharedlib.annotation.ClientCall;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientCallRuntimeAspectTest {

    private static final String METHOD = "POST";
    private static final String DESTINATION_URL = "https://orders.example/api";

    @Mock ClientApiRuntimePolicyService policyService;
    @Mock ProceedingJoinPoint joinPoint;
    @Mock SecurityAuditLogger auditLogger;

    List<Long> sleepCalls;
    ClientCallRuntimeAspect aspect;
    ClientApiRuntimeConfig config;
    ClientCall clientCall;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        sleepCalls = new ArrayList<>();
        aspect = new ClientCallRuntimeAspect(policyService, auditLogger, sleepCalls::add);
        config = ClientApiRuntimeConfig.builder()
                .id(UUID.randomUUID())
                .endpointId(UUID.randomUUID())
                .method(METHOD)
                .destinationUrl(DESTINATION_URL)
                .protocol("HTTP")
                .enabled(true)
                .syncStatus("ACTIVE")
                .maxRetries(0)
                .retryDelayMs(0)
                .failureAction("IGNORE")
                .latencyThresholdMs(60_000)
                .build();
        clientCall = annotation("annotatedClientCall");
        when(policyService.resolve(METHOD, DESTINATION_URL)).thenReturn(config);
    }

    @Test
    void aroundClientCall_shouldReturnJoinPointResultOnSuccess() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        var result = aspect.aroundClientCall(joinPoint, clientCall);

        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(event ->
                "OUTBOUND_HTTP".equals(event.getFlowType()) && "SUCCESS".equals(event.getStatus())));
        assertThat(sleepCalls).isEmpty();
    }

    @Test
    void aroundClientCall_shouldRetryRuntimeFailureByMaxRetriesAndSleepBetweenAttempts() throws Throwable {
        config.setMaxRetries(2);
        config.setRetryDelayMs(25);
        when(joinPoint.proceed())
                .thenThrow(new RuntimeException("network-1"))
                .thenThrow(new RuntimeException("network-2"))
                .thenReturn("ok");

        var result = aspect.aroundClientCall(joinPoint, clientCall);

        assertThat(result).isEqualTo("ok");
        verify(joinPoint, times(3)).proceed();
        verify(auditLogger, times(2)).log(org.mockito.ArgumentMatchers.argThat(event ->
                "OUTBOUND_HTTP".equals(event.getFlowType()) && "RETRY".equals(event.getStatus())));
        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(event ->
                "OUTBOUND_HTTP".equals(event.getFlowType()) && "SUCCESS".equals(event.getStatus()) && event.getRetryAttempt() == 3));
        assertThat(sleepCalls).containsExactly(25L, 25L);
    }

    @Test
    void aroundClientCall_shouldPropagateOriginalFailureForIgnoreWhenRetriesExhausted() throws Throwable {
        config.setMaxRetries(1);
        config.setRetryDelayMs(10);
        var failure = new RuntimeException("network");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.aroundClientCall(joinPoint, clientCall))
                .isSameAs(failure);
        verify(joinPoint, times(2)).proceed();
        assertThat(sleepCalls).containsExactly(10L);
    }

    @Test
    void aroundClientCall_shouldWrapFinalFailureForCompensate() throws Throwable {
        config.setFailureAction("COMPENSATE");
        var failure = new RuntimeException("network");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.aroundClientCall(joinPoint, clientCall))
                .isInstanceOf(OutboundException.class)
                .hasCause(failure)
                .extracting("errorCode")
                .isEqualTo(OutboundErrorCode.RETRY_EXHAUSTED);
    }

    @Test
    void aroundClientCall_shouldNotRetryPolicyValidationFailures() throws Throwable {
        var failure = new OutboundException(OutboundErrorCode.CONFIG_DISABLED, "disabled");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.aroundClientCall(joinPoint, clientCall))
                .isSameAs(failure);
        verify(joinPoint).proceed();
        assertThat(sleepCalls).isEmpty();
    }

    @Test
    void aroundClientCall_shouldEvaluateLatencyThresholdWithoutChangingSuccessfulResult() throws Throwable {
        config.setLatencyThresholdMs(0);
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.aroundClientCall(joinPoint, clientCall)).isEqualTo("ok");
    }

    @Test
    void aroundClientCall_shouldRestoreInterruptFlagWhenRetrySleepIsInterrupted() throws Throwable {
        config.setMaxRetries(1);
        config.setRetryDelayMs(10);
        aspect = new ClientCallRuntimeAspect(policyService, auditLogger, millis -> {
            throw new InterruptedException("stop");
        });
        var failure = new RuntimeException("network");
        when(joinPoint.proceed()).thenThrow(failure);

        try {
            assertThatThrownBy(() -> aspect.aroundClientCall(joinPoint, clientCall))
                    .isInstanceOf(InterruptedException.class)
                    .hasMessage("stop");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        verify(joinPoint).proceed();
    }

    private ClientCall annotation(String methodName) throws NoSuchMethodException {
        Method method = TestClient.class.getDeclaredMethod(methodName);
        return method.getAnnotation(ClientCall.class);
    }

    private static class TestClient {
        @ClientCall(name = "orders", method = METHOD, destinationUrl = DESTINATION_URL)
        void annotatedClientCall() {
        }
    }
}
