package com.pm.be.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.be.service.sync.SyncRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
            var endpointId = optionalUuid(node, "endpointId");
            if (endpointId == null) {
                log.warn("Skip exposedApi because endpointId is missing: name={}", optionalText(node, "name"));
                continue;
            }
            list.add(new SyncRegistrationService.ExposedApiInfo(
                    endpointId,
                    optionalText(node, "endpointKey"),
                    optionalText(node, "name"),
                    optionalText(node, "path"),
                    optionalText(node, "topic"),
                    optionalText(node, "method"),
                    optionalText(node, "protocol")));
        }
        return list;
    }

    private List<SyncRegistrationService.ClientApiInfo> parseClientApis(JsonNode array) {
        if (array == null || !array.isArray()) return null;
        var list = new ArrayList<SyncRegistrationService.ClientApiInfo>();
        for (var node : array) {
            var endpointId = optionalUuid(node, "endpointId");
            if (endpointId == null) {
                log.warn("Skip clientApi because endpointId is missing: name={}", optionalText(node, "name"));
                continue;
            }
            list.add(new SyncRegistrationService.ClientApiInfo(
                    endpointId,
                    optionalText(node, "endpointKey"),
                    optionalText(node, "name"),
                    optionalText(node, "destinationUrl"),
                    optionalText(node, "topic"),
                    optionalText(node, "method"),
                    optionalText(node, "protocol")));
        }
        return list;
    }

    private UUID optionalUuid(JsonNode root, String fieldName) {
        var value = optionalText(root, fieldName);
        return StringUtils.hasText(value) ? UUID.fromString(value) : null;
    }

    private String optionalText(JsonNode root, String fieldName) {
        var node = root.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }

    private String requiredText(JsonNode root, String fieldName) {
        var node = root.get(fieldName);
        if (node == null || !StringUtils.hasText(node.asText())) {
            throw new IllegalArgumentException("Registration event missing required field: " + fieldName);
        }
        return node.asText();
    }
}
