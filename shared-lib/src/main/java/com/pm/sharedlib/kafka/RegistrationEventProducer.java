package com.pm.sharedlib.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.sharedlib.model.ServiceRegistrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
public class RegistrationEventProducer {

    private static final String TOPIC = "vdt.service.registration";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(ServiceRegistrationEvent event) {
        try {
            var json = objectMapper.writeValueAsString(event);

            System.out.println(">>> Kafka SEND: " + json);

            kafkaTemplate.send(TOPIC, event.getServiceName(), json) .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("Sent successfully");
                } else {
                    ex.printStackTrace();
                }
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
