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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientCallRuntimeAspectMqTest {

    private static final String TOPIC = "orders.created";

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
                .apiName("publish-orders")
                .topic(TOPIC)
                .protocol("MQ")
                .enabled(true)
                .syncStatus("ACTIVE")
                .timeoutMs(1000)
                .maxRetries(0)
                .retryDelayMs(0)
                .failureAction("IGNORE")
                .latencyThresholdMs(60_000)
                .build();
        clientCall = annotation("mqClientCall");
        when(policyService.resolveMq(TOPIC)).thenReturn(config);
    }

    @Test
    void aroundClientCall_shouldWaitCompletableFutureAckAndReturnOriginalFuture() throws Throwable {
        var future = CompletableFuture.completedFuture("ack");
        when(joinPoint.proceed()).thenReturn(future);

        var result = aspect.aroundClientCall(joinPoint, clientCall);

        assertThat(result).isSameAs(future);
        verify(joinPoint).proceed();
        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(event ->
                "OUTBOUND_MQ".equals(event.getFlowType()) && "SUCCESS".equals(event.getStatus())));
        assertThat(sleepCalls).isEmpty();
    }

    @Test
    void aroundClientCall_shouldRetryTimeoutWaitingForAck() throws Throwable {
        config.setTimeoutMs(1);
        config.setMaxRetries(1);
        config.setRetryDelayMs(25);
        var neverCompletes = new CompletableFuture<>();
        var success = CompletableFuture.completedFuture("ack");
        when(joinPoint.proceed()).thenReturn(neverCompletes).thenReturn(success);

        var result = aspect.aroundClientCall(joinPoint, clientCall);

        assertThat(result).isSameAs(success);
        verify(joinPoint, times(2)).proceed();
        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(event ->
                "OUTBOUND_MQ".equals(event.getFlowType()) && "TIMEOUT".equals(event.getStatus())));
        verify(auditLogger).log(org.mockito.ArgumentMatchers.argThat(event ->
                "OUTBOUND_MQ".equals(event.getFlowType()) && "SUCCESS".equals(event.getStatus()) && event.getRetryAttempt() == 2));
        assertThat(sleepCalls).containsExactly(25L);
    }

    @Test
    void aroundClientCall_shouldThrowCompensationExceptionWithTimeoutCodeAfterRetriesExhausted() throws Throwable {
        config.setTimeoutMs(1);
        config.setFailureAction("COMPENSATE");
        var neverCompletes = new CompletableFuture<>();
        when(joinPoint.proceed()).thenReturn(neverCompletes);

        assertThatThrownBy(() -> aspect.aroundClientCall(joinPoint, clientCall))
                .isInstanceOf(OutboundException.class)
                .hasCauseInstanceOf(TimeoutException.class)
                .extracting("errorCode")
                .isEqualTo(OutboundErrorCode.TIMEOUT_EXCEEDED);
    }

    private ClientCall annotation(String methodName) throws NoSuchMethodException {
        Method method = TestClient.class.getDeclaredMethod(methodName);
        return method.getAnnotation(ClientCall.class);
    }

    private static class TestClient {
        @ClientCall(name = "publish-orders", protocol = "MQ", topic = TOPIC)
        void mqClientCall() {
        }
    }
}
