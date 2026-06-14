package com.pm.sharedlib.runtime;

import com.pm.sharedlib.annotation.ClientCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class ClientCallRuntimeAspect {

    private static final String FAILURE_ACTION_COMPENSATE = "COMPENSATE";

    private final ClientApiRuntimePolicyService policyService;
    private final Sleeper sleeper;

    public ClientCallRuntimeAspect(ClientApiRuntimePolicyService policyService) {
        this(policyService, Thread::sleep);
    }

    @Around("@annotation(clientCall)")
    public Object aroundClientCall(ProceedingJoinPoint joinPoint, ClientCall clientCall) throws Throwable {
        var config = policyService.resolve(clientCall.method(), clientCall.destinationUrl());
        var maxRetries = positiveOrZero(config.getMaxRetries());
        var maxAttempts = 1 + maxRetries;
        var retryDelayMs = positiveOrZero(config.getRetryDelayMs());
        Throwable lastFailure = null;

        for (var attempt = 1; attempt <= maxAttempts; attempt++) {
            var startedAt = System.nanoTime();
            try {
                var result = joinPoint.proceed();
                logDuration(config, clientCall, attempt, startedAt, true, null);
                return result;
            } catch (Throwable failure) {
                lastFailure = failure;
                var retryable = isRetryable(failure);
                logDuration(config, clientCall, attempt, startedAt, false, failure);
                if (!retryable || attempt >= maxAttempts) {
                    log.warn("outbound_client_call_retry_exhausted method={} destinationUrl={} attempts={} retryable={} error={}",
                            clientCall.method(), clientCall.destinationUrl(), attempt, retryable, failure.toString());
                    throw handleFinalFailure(config, failure);
                }
                log.warn("outbound_client_call_retry method={} destinationUrl={} attempt={} maxAttempts={} delayMs={} error={}",
                        clientCall.method(), clientCall.destinationUrl(), attempt, maxAttempts, retryDelayMs, failure.toString());
                try {
                    sleep(retryDelayMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw handleFinalFailure(config, interrupted);
                }
            }
        }

        throw handleFinalFailure(config, lastFailure);
    }

    private void logDuration(
            ClientApiRuntimeConfig config,
            ClientCall clientCall,
            int attempt,
            long startedAt,
            boolean success,
            Throwable failure) {
        var durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (success) {
            log.info("outbound_client_call_success method={} destinationUrl={} attempt={} durationMs={}",
                    clientCall.method(), clientCall.destinationUrl(), attempt, durationMs);
        } else {
            log.warn("outbound_client_call_failure method={} destinationUrl={} attempt={} durationMs={} error={}",
                    clientCall.method(), clientCall.destinationUrl(), attempt, durationMs, failure.toString());
            if (isTimeout(failure)) {
                log.warn("outbound_client_call_timeout method={} destinationUrl={} attempt={} durationMs={}",
                        clientCall.method(), clientCall.destinationUrl(), attempt, durationMs);
            }
        }
        var latencyThresholdMs = config.getLatencyThresholdMs();
        if (latencyThresholdMs != null && latencyThresholdMs >= 0 && durationMs > latencyThresholdMs) {
            log.warn("outbound_client_call_latency_exceeded method={} destinationUrl={} attempt={} durationMs={} thresholdMs={}",
                    clientCall.method(), clientCall.destinationUrl(), attempt, durationMs, latencyThresholdMs);
        }
    }

    private Throwable handleFinalFailure(ClientApiRuntimeConfig config, Throwable failure) {
        if (FAILURE_ACTION_COMPENSATE.equalsIgnoreCase(config.getFailureAction())) {
            return new OutboundException(
                    OutboundErrorCode.RETRY_EXHAUSTED,
                    "Outbound ClientApi call failed after configured attempts",
                    failure);
        }
        return failure;
    }

    private boolean isRetryable(Throwable failure) {
        if (failure instanceof OutboundException) {
            return false;
        }
        if (failure instanceof HttpStatusCodeException) {
            return true;
        }
        return failure instanceof RuntimeException || isTimeout(failure);
    }

    private boolean isTimeout(Throwable failure) {
        var current = failure;
        while (current != null) {
            if (current instanceof TimeoutException || current instanceof SocketTimeoutException) {
                return true;
            }
            if (current instanceof ResourceAccessException && current.getCause() instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleep(long retryDelayMs) throws InterruptedException {
        if (retryDelayMs <= 0) {
            return;
        }
        sleeper.sleep(retryDelayMs);
    }

    private int positiveOrZero(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
