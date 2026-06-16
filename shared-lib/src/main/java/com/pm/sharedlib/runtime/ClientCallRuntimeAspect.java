package com.pm.sharedlib.runtime;

import com.pm.sharedlib.annotation.ClientCall;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.SerializationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Aspect
@Slf4j
public class ClientCallRuntimeAspect {

    private static final String FAILURE_ACTION_COMPENSATE = "COMPENSATE";
    private static final String MQ_PROTOCOL = "MQ";
    private static final String REGISTRATION_TOPIC = "vdt.service.registration";

    private final ClientApiRuntimePolicyService policyService;
    private final Sleeper sleeper;
    private final SecurityAuditLogger auditLogger;

    public ClientCallRuntimeAspect(ClientApiRuntimePolicyService policyService) {
        this(policyService, null, Thread::sleep);
    }

    public ClientCallRuntimeAspect(ClientApiRuntimePolicyService policyService, SecurityAuditLogger auditLogger) {
        this(policyService, auditLogger, Thread::sleep);
    }

    public ClientCallRuntimeAspect(ClientApiRuntimePolicyService policyService, Sleeper sleeper) {
        this(policyService, null, sleeper);
    }

    public ClientCallRuntimeAspect(ClientApiRuntimePolicyService policyService, SecurityAuditLogger auditLogger, Sleeper sleeper) {
        this.policyService = policyService;
        this.auditLogger = auditLogger;
        this.sleeper = sleeper;
    }

    @Around("@annotation(clientCall)")
    public Object aroundClientCall(ProceedingJoinPoint joinPoint, ClientCall clientCall) throws Throwable {
        if (MQ_PROTOCOL.equalsIgnoreCase(clientCall.protocol())) {
            return aroundMqClientCall(joinPoint, clientCall);
        }

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
                auditHttp(config, clientCall, "SUCCESS", "SUCCESS", null, null, attempt, maxRetries, retryDelayMs, startedAt);
                return result;
            } catch (Throwable failure) {
                lastFailure = failure;
                var retryable = isRetryable(failure);
                logDuration(config, clientCall, attempt, startedAt, false, failure);
                if (!retryable || attempt >= maxAttempts) {
                    auditHttp(config, clientCall, "FAILED", isTimeout(failure) ? "TIMEOUT_EXCEEDED" : "RETRY_EXHAUSTED",
                            isTimeout(failure) ? "TIMEOUT_EXCEEDED" : failure.getClass().getSimpleName(), failure.getMessage(),
                            attempt, maxRetries, retryDelayMs, startedAt);
                    log.warn("outbound_client_call_retry_exhausted method={} destinationUrl={} attempts={} retryable={} error={}",
                            clientCall.method(), clientCall.destinationUrl(), attempt, retryable, failure.toString());
                    throw handleFinalFailure(config, failure);
                }
                auditHttp(config, clientCall, isTimeout(failure) ? "TIMEOUT" : "RETRY",
                        isTimeout(failure) ? "TIMEOUT_EXCEEDED" : "RETRY_SCHEDULED",
                        isTimeout(failure) ? "TIMEOUT_EXCEEDED" : failure.getClass().getSimpleName(), failure.getMessage(),
                        attempt, maxRetries, retryDelayMs, startedAt);
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

    private Object aroundMqClientCall(ProceedingJoinPoint joinPoint, ClientCall clientCall) throws Throwable {
        if (REGISTRATION_TOPIC.equals(clientCall.topic())) {
            return joinPoint.proceed();
        }

        var config = policyService.resolveMq(clientCall.topic());
        var maxRetries = positiveOrZero(config.getMaxRetries());
        var maxAttempts = 1 + maxRetries;
        var retryDelayMs = positiveOrZero(config.getRetryDelayMs());
        Throwable lastFailure = null;

        for (var attempt = 1; attempt <= maxAttempts; attempt++) {
            var startedAt = System.nanoTime();
            try {
                var result = joinPoint.proceed();
                waitPublishAckIfRequired(result, config.getTimeoutMs());
                logMqDuration(config, clientCall, attempt, startedAt, true, null, false);
                auditMq(config, clientCall, "SUCCESS", "SUCCESS", null, null, attempt, maxRetries, retryDelayMs, startedAt);
                return result;
            } catch (Throwable failure) {
                lastFailure = unwrapExecutionFailure(failure);
                var retryable = isMqRetryable(lastFailure);
                logMqDuration(config, clientCall, attempt, startedAt, false, lastFailure, false);
                var outboundError = classifyMqOutboundError(lastFailure);
                if (!retryable || attempt >= maxAttempts) {
                    auditMq(config, clientCall, isTimeout(lastFailure) ? "TIMEOUT" : "FAILED",
                            outboundError.name(), outboundError.name(), lastFailure.getMessage(),
                            attempt, maxRetries, retryDelayMs, startedAt);
                    log.warn("outbound_client_mq_retry_exhausted topic={} attempts={} retryable={} errorCode={} error={}",
                            clientCall.topic(), attempt, retryable, outboundError, lastFailure.toString());
                    throw handleFinalFailure(config, outboundError, lastFailure);
                }
                auditMq(config, clientCall, isTimeout(lastFailure) ? "TIMEOUT" : "RETRY",
                        isTimeout(lastFailure) ? "TIMEOUT_EXCEEDED" : "RETRY_SCHEDULED", outboundError.name(), lastFailure.getMessage(),
                        attempt, maxRetries, retryDelayMs, startedAt);
                log.warn("outbound_client_mq_retry topic={} attempt={} maxAttempts={} delayMs={} errorCode={} error={}",
                        clientCall.topic(), attempt, maxAttempts, retryDelayMs, outboundError, lastFailure.toString());
                try {
                    sleep(retryDelayMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw handleFinalFailure(config, OutboundErrorCode.INVOCATION_FAILED, interrupted);
                }
            }
        }

        throw handleFinalFailure(config, classifyMqOutboundError(lastFailure), lastFailure);
    }

    private void waitPublishAckIfRequired(Object result, Integer timeoutMs) throws Exception {
        if (result == null) {
            log.warn("outbound_client_mq_publish_invoked status=PUBLISH_INVOKED ackObservable=false");
            return;
        }
        if (result instanceof SendResult<?, ?>) {
            return;
        }
        if (result instanceof Future<?> future) {
            waitFuture(future, timeoutMs);
            return;
        }
        log.warn("outbound_client_mq_publish_invoked status=PUBLISH_INVOKED ackObservable=false resultType={}",
                result.getClass().getName());
    }

    private void waitFuture(Future<?> future, Integer timeoutMs) throws Exception {
        if (timeoutMs == null || timeoutMs < 0) {
            throw new OutboundException(OutboundErrorCode.CONFIG_MISSING, "ClientApi MQ timeoutMs must be configured");
        }
        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex) {
            var cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw ex;
        }
    }

    private void logMqDuration(
            ClientApiRuntimeConfig config,
            ClientCall clientCall,
            int attempt,
            long startedAt,
            boolean success,
            Throwable failure,
            boolean publishInvokedOnly) {
        var durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (success) {
            log.info("outbound_client_mq_success endpointId={} apiName={} protocol=MQ topic={} durationMs={} timeoutMs={} maxRetries={} retryAttempt={} retryDelayMs={} failureAction={}",
                    config.getEndpointId(), config.getApiName(), clientCall.topic(), durationMs, config.getTimeoutMs(),
                    config.getMaxRetries(), attempt, config.getRetryDelayMs(), config.getFailureAction());
        } else {
            var errorCode = classifyMqOutboundError(failure);
            log.warn("outbound_client_mq_failure endpointId={} apiName={} protocol=MQ topic={} durationMs={} timeoutMs={} maxRetries={} retryAttempt={} retryDelayMs={} failureAction={} errorCode={} error={}",
                    config.getEndpointId(), config.getApiName(), clientCall.topic(), durationMs, config.getTimeoutMs(),
                    config.getMaxRetries(), attempt, config.getRetryDelayMs(), config.getFailureAction(), errorCode, failure.toString());
            if (isTimeout(failure)) {
                log.warn("outbound_client_mq_timeout endpointId={} topic={} durationMs={} timeoutMs={} retryAttempt={} errorCode={}",
                        config.getEndpointId(), clientCall.topic(), durationMs, config.getTimeoutMs(), attempt,
                        OutboundErrorCode.TIMEOUT_EXCEEDED);
            }
        }
        var latencyThresholdMs = config.getLatencyThresholdMs();
        if (!publishInvokedOnly && latencyThresholdMs != null && latencyThresholdMs >= 0 && durationMs > latencyThresholdMs) {
            log.warn("outbound_client_mq_latency_exceeded endpointId={} topic={} durationMs={} latencyThresholdMs={} errorCode=LATENCY_THRESHOLD_EXCEEDED",
                    config.getEndpointId(), clientCall.topic(), durationMs, latencyThresholdMs);
        }
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

    private void auditHttp(ClientApiRuntimeConfig config, ClientCall clientCall, String status, String resultCode,
                           String errorCode, String denyReason, int attempt, int maxRetries, int retryDelayMs, long startedAt) {
        audit(SecurityLogEvent.builder()
                .timestamp(Instant.now())
                .serviceName(config.getServiceName())
                .endpointId(config.getEndpointId())
                .endpointName(config.getApiName())
                .flowType("OUTBOUND_HTTP")
                .direction("OUTBOUND")
                .protocol("HTTP")
                .method(clientCall.method())
                .targetUrl(clientCall.destinationUrl())
                .status(status)
                .resultCode(resultCode)
                .errorCode(errorCode)
                .denyReason(denyReason)
                .durationMs((System.nanoTime() - startedAt) / 1_000_000)
                .latencyThresholdMs(config.getLatencyThresholdMs())
                .timeoutMs(config.getTimeoutMs())
                .retryAttempt(attempt)
                .maxRetries(maxRetries)
                .retryDelayMs(retryDelayMs)
                .failureAction(config.getFailureAction())
                .retentionDays(SecurityLogRetentionBucketMapper.normalizedDays(config.getLogRetentionDays()))
                .retentionBucket(SecurityLogRetentionBucketMapper.bucket(config.getLogRetentionDays()))
                .build());
    }

    private void auditMq(ClientApiRuntimeConfig config, ClientCall clientCall, String status, String resultCode,
                         String errorCode, String denyReason, int attempt, int maxRetries, int retryDelayMs, long startedAt) {
        audit(SecurityLogEvent.builder()
                .timestamp(Instant.now())
                .serviceName(config.getServiceName())
                .endpointId(config.getEndpointId())
                .endpointName(config.getApiName())
                .flowType("OUTBOUND_MQ")
                .direction("OUTBOUND")
                .protocol("MQ")
                .topic(clientCall.topic())
                .status(status)
                .resultCode(resultCode)
                .errorCode(errorCode)
                .denyReason(denyReason)
                .durationMs((System.nanoTime() - startedAt) / 1_000_000)
                .latencyThresholdMs(config.getLatencyThresholdMs())
                .timeoutMs(config.getTimeoutMs())
                .retryAttempt(attempt)
                .maxRetries(maxRetries)
                .retryDelayMs(retryDelayMs)
                .failureAction(config.getFailureAction())
                .retentionDays(SecurityLogRetentionBucketMapper.normalizedDays(config.getLogRetentionDays()))
                .retentionBucket(SecurityLogRetentionBucketMapper.bucket(config.getLogRetentionDays()))
                .build());
    }

    private void audit(SecurityLogEvent event) {
        if (auditLogger == null) {
            return;
        }
        try {
            auditLogger.log(event);
        } catch (RuntimeException e) {
            log.warn("security_audit_emit_failed flowType={} endpointId={}", event.getFlowType(), event.getEndpointId(), e);
        }
    }

    private Throwable handleFinalFailure(ClientApiRuntimeConfig config, Throwable failure) {
        return handleFinalFailure(config, OutboundErrorCode.RETRY_EXHAUSTED, failure);
    }

    private Throwable handleFinalFailure(ClientApiRuntimeConfig config, OutboundErrorCode errorCode, Throwable failure) {
        if (FAILURE_ACTION_COMPENSATE.equalsIgnoreCase(config.getFailureAction())) {
            return new OutboundException(
                    errorCode,
                    "Outbound ClientApi call failed after configured attempts",
                    failure);
        }
        return failure;
    }

    private OutboundErrorCode classifyMqOutboundError(Throwable failure) {
        var current = failure;
        while (current != null) {
            if (current instanceof OutboundException outboundException) {
                return outboundException.getErrorCode();
            }
            if (current instanceof TimeoutException || current instanceof SocketTimeoutException) {
                return OutboundErrorCode.TIMEOUT_EXCEEDED;
            }
            if (current instanceof SerializationException) {
                return OutboundErrorCode.SERIALIZATION_ERROR;
            }
            if (current instanceof KafkaException) {
                return OutboundErrorCode.PRODUCER_EXCEPTION;
            }
            current = current.getCause();
        }
        return OutboundErrorCode.PUBLISH_FAILED;
    }

    private Throwable unwrapExecutionFailure(Throwable failure) {
        if (failure instanceof ExecutionException && failure.getCause() != null) {
            return failure.getCause();
        }
        return failure;
    }

    private boolean isMqRetryable(Throwable failure) {
        if (failure instanceof OutboundException || isSerializationFailure(failure)) {
            return false;
        }
        return isTimeout(failure) || failure instanceof KafkaException || failure instanceof RuntimeException;
    }

    private boolean isSerializationFailure(Throwable failure) {
        var current = failure;
        while (current != null) {
            if (current instanceof SerializationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
