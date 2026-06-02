package com.pm.be.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.service.SyncRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationEventConsumer {

    private final SyncRegistrationService syncService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "vdt.service.registration", groupId = "management-system")
    public void handle(String eventJson) {
        try {
            log.debug("Raw event JSON: {}", eventJson);
            var root = objectMapper.readTree(eventJson);

            String serviceName = requiredText(root, "serviceName");
            String serviceUrl = requiredText(root, "serviceUrl");
            var exposedApisNode = root.get("exposedApis");
            if (exposedApisNode == null || !exposedApisNode.isArray()) {
                log.warn("Skip registration event because exposedApis is missing or invalid: serviceName={}", serviceName);
                return;
            }

            log.info("Received registration event: {}", serviceName);

            var exposedApis = parseExposedApis(exposedApisNode);
            var clientApis = parseClientApis(root.get("clientApis"));

            syncService.sync(serviceName, serviceUrl, exposedApis, clientApis);
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

    private String requiredText(JsonNode root, String fieldName) {
        var node = root.get(fieldName);
        if (node == null || !StringUtils.hasText(node.asText())) {
            throw new IllegalArgumentException("Registration event missing required field: " + fieldName);
        }
        return node.asText();
    }
}
