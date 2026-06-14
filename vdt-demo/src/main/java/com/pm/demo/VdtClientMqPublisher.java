package com.pm.demo;

import com.pm.sharedlib.annotation.ClientCall;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class VdtClientMqPublisher {

    static final String VDT_CLIENT_ORDER_TOPIC = "vdt-client.orders";
    static final String VDT_CLIENT_ORDER_FAILURE_TOPIC = "vdt-client.orders.failure";
    static final String VDT_CLIENT_ORDER_TIMEOUT_TOPIC = "vdt-client.orders.timeout";
    static final String VDT_CLIENT_ORDER_RETRY_TOPIC = "vdt-client.orders.retry";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AtomicInteger retryAttempts = new AtomicInteger();

    @ClientCall(name = "publish-vdt-client-order-mq", protocol = "MQ", topic = VDT_CLIENT_ORDER_TOPIC)
    public CompletableFuture<SendResult<String, String>> publishOrder(String payload) {
        var record = new ProducerRecord<String, String>(VDT_CLIENT_ORDER_TOPIC, payload);
        return kafkaTemplate.send(record);
    }

    @ClientCall(name = "publish-vdt-client-order-mq-failure", protocol = "MQ", topic = VDT_CLIENT_ORDER_FAILURE_TOPIC)
    public CompletableFuture<SendResult<String, String>> publishOrderFailure(String payload) {
        return CompletableFuture.failedFuture(new KafkaException("Simulated MQ outbound producer failure: " + payload));
    }

    @ClientCall(name = "publish-vdt-client-order-mq-timeout", protocol = "MQ", topic = VDT_CLIENT_ORDER_TIMEOUT_TOPIC)
    public CompletableFuture<SendResult<String, String>> publishOrderTimeout(String payload) {
        return new CompletableFuture<>();
    }

    void resetRetryAttempts() {
        retryAttempts.set(0);
    }

    @ClientCall(name = "publish-vdt-client-order-mq-retry", protocol = "MQ", topic = VDT_CLIENT_ORDER_RETRY_TOPIC)
    public CompletableFuture<SendResult<String, String>> publishOrderWithRetry(String payload, int failTimes) {
        var attempt = retryAttempts.incrementAndGet();
        if (attempt <= failTimes) {
            return CompletableFuture.failedFuture(new KafkaException(
                    "Simulated MQ outbound retry failure attempt=" + attempt + ", failTimes=" + failTimes));
        }
        var record = new ProducerRecord<String, String>(VDT_CLIENT_ORDER_RETRY_TOPIC, payload);
        return kafkaTemplate.send(record);
    }
}
