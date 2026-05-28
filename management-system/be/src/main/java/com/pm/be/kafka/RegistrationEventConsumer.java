package com.pm.be.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.service.SyncRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationEventConsumer {

    private final SyncRegistrationService syncService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "vdt.service.registration", groupId = "management-system")
    public void handle(String eventJson) {
        try {
            log.debug("Raw event JSON: {}", eventJson);
            var root = objectMapper.readTree(eventJson);

            String serviceName = root.get("serviceName").asText();
            String serviceUrl = root.get("serviceUrl").asText();
            String keyService = root.get("keyService").asText();

            log.info("Received registration event: {}", serviceName);

            var exposedApis = parseExposedApis(root.get("exposedApis"));
            var clientApis = parseClientApis(root.get("clientApis"));

            syncService.sync(serviceName, serviceUrl, keyService, exposedApis, clientApis);
        } catch (Exception e) {
            log.error("Failed to process registration event", e);
        }
    }

    private List<SyncRegistrationService.ExposedApiInfo> parseExposedApis(JsonNode array) {
        if (array == null || !array.isArray()) return null;
        var list = new ArrayList<SyncRegistrationService.ExposedApiInfo>();
        for (var node : array) {
            list.add(new SyncRegistrationService.ExposedApiInfo(
                    node.get("name").asText(),
                    node.get("path").asText(),
                    node.get("method").asText(),
                    node.get("protocol").asText()));
        }
        return list;
    }

    private List<SyncRegistrationService.ClientApiInfo> parseClientApis(JsonNode array) {
        if (array == null || !array.isArray()) return null;
        var list = new ArrayList<SyncRegistrationService.ClientApiInfo>();
        for (var node : array) {
            list.add(new SyncRegistrationService.ClientApiInfo(
                    node.get("name").asText(),
                    node.get("destinationUrl").asText(),
                    node.get("method").asText(),
                    node.get("protocol").asText()));
        }
        return list;
    }
}
