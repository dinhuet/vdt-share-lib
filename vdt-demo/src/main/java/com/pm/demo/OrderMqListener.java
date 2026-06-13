package com.pm.demo;

import com.pm.sharedlib.annotation.SharedApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderMqListener {

    @SharedApi(name = "create-order-mq", protocol = "MQ", topic = "demo.orders")
    @KafkaListener(topics = "demo.orders", groupId = "vdt-demo")
    public void onCreateOrder(ConsumerRecord<String, String> record) {
        log.info("Received order: key={}, value={}, topic={}, partition={}, offset={}",
                record.key(), record.value(), record.topic(),
                record.partition(), record.offset());
    }
}
