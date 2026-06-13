package com.pm.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderMqController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping("/send-order")
    public String sendOrder(
            @RequestBody String order,
            @RequestHeader(value = "X-Client-Id", required = false) String clientId,
            @RequestHeader(value = "X-Key-Id", required = false) String keyId,
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey) {
        kafkaTemplate.send("demo.orders", order);
        return "sent: " + order;
    }
}
