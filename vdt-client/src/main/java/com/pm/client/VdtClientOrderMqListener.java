package com.pm.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VdtClientOrderMqListener {

    static final String VDT_CLIENT_ORDER_TOPIC = "vdt-client.orders";
    static final String VDT_CLIENT_ORDER_RETRY_TOPIC = "vdt-client.orders.retry";

    @KafkaListener(topics = {VDT_CLIENT_ORDER_TOPIC, VDT_CLIENT_ORDER_RETRY_TOPIC}, groupId = "vdt-client-mq-demo")
    public void onOrder(ConsumerRecord<String, String> record) {
        log.info("Received order from MQ: key={}, value={}, topic={}, partition={}, offset={}",
                record.key(), record.value(), record.topic(), record.partition(), record.offset());
    }
}
