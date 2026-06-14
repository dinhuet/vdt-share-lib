package com.pm.sharedlib.runtime;

import com.pm.sharedlib.endpoint.EndpointDefinition;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class ClientApiRuntimePolicyService {

    private static final String ACTIVE_SYNC_STATUS = "ACTIVE";
    private static final String HTTP_PROTOCOL = "HTTP";
    private static final String MQ_PROTOCOL = "MQ";
    private static final String WEBHOOK_PROTOCOL = "WEBHOOK";

    private final EndpointRegistry endpointRegistry;
    private final SecuritySettingsStore settingsStore;

    public ClientApiRuntimeConfig resolve(String method, String destinationUrl) {
        validateInput(method, destinationUrl);
        var endpoint = endpointRegistry.findClientHttp(method, destinationUrl)
                .orElseThrow(() -> new OutboundException(
                        OutboundErrorCode.ENDPOINT_NOT_REGISTERED,
                        "Client HTTP endpoint is not registered: method=" + method + ", destinationUrl=" + destinationUrl));
        var config = settingsStore.getClientApi(endpoint.getEndpointId())
                .orElseThrow(() -> new OutboundException(
                        OutboundErrorCode.CONFIG_MISSING,
                        "ClientApi runtime config is missing for endpointId=" + endpoint.getEndpointId()));
        validate(endpoint, config, method, destinationUrl);
        return config;
    }

    public ClientApiRuntimeConfig resolveMq(String topic) {
        validateMqInput(topic);
        var endpoint = endpointRegistry.findClientMq(topic)
                .orElseThrow(() -> new OutboundException(
                        OutboundErrorCode.ENDPOINT_NOT_REGISTERED,
                        "Client MQ endpoint is not registered: topic=" + topic));
        var config = settingsStore.getClientApi(endpoint.getEndpointId())
                .orElseThrow(() -> new OutboundException(
                        OutboundErrorCode.CONFIG_MISSING,
                        "ClientApi runtime config is missing for endpointId=" + endpoint.getEndpointId()));
        validateMq(endpoint, config, topic);
        return config;
    }

    private void validateInput(String method, String destinationUrl) {
        if (!StringUtils.hasText(method)) {
            throw new OutboundException(OutboundErrorCode.METHOD_MISMATCH, "ClientCall method must not be blank");
        }
        if (!StringUtils.hasText(destinationUrl)) {
            throw new OutboundException(OutboundErrorCode.DESTINATION_URL_MISMATCH, "ClientCall destinationUrl must not be blank");
        }
    }

    private void validateMqInput(String topic) {
        if (!StringUtils.hasText(topic)) {
            throw new OutboundException(OutboundErrorCode.TOPIC_MISMATCH, "ClientCall topic must not be blank");
        }
    }

    private void validate(EndpointDefinition endpoint, ClientApiRuntimeConfig config, String method, String destinationUrl) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new OutboundException(OutboundErrorCode.CONFIG_DISABLED, "ClientApi runtime config is disabled");
        }
        if (!ACTIVE_SYNC_STATUS.equalsIgnoreCase(config.getSyncStatus())) {
            throw new OutboundException(OutboundErrorCode.CONFIG_INACTIVE, "ClientApi runtime config is not active");
        }
        if (!isHttpLike(endpoint.getProtocol())
                || !isHttpLike(config.getProtocol())
                || !endpoint.getProtocol().equalsIgnoreCase(config.getProtocol())) {
            throw new OutboundException(OutboundErrorCode.PROTOCOL_MISMATCH, "ClientApi protocol mismatch");
        }
        if (!method.equalsIgnoreCase(endpoint.getMethod()) || !method.equalsIgnoreCase(config.getMethod())) {
            throw new OutboundException(OutboundErrorCode.METHOD_MISMATCH, "ClientApi method mismatch");
        }
        if (!destinationUrl.equals(endpoint.getDestinationUrl()) || !destinationUrl.equals(config.getDestinationUrl())) {
            throw new OutboundException(OutboundErrorCode.DESTINATION_URL_MISMATCH, "ClientApi destinationUrl mismatch");
        }
    }

    private void validateMq(EndpointDefinition endpoint, ClientApiRuntimeConfig config, String topic) {
        validateCommonRuntimeState(config);
        if (!MQ_PROTOCOL.equalsIgnoreCase(endpoint.getProtocol()) || !MQ_PROTOCOL.equalsIgnoreCase(config.getProtocol())) {
            throw new OutboundException(OutboundErrorCode.PROTOCOL_MISMATCH, "ClientApi protocol mismatch");
        }
        if (!topic.equals(endpoint.getTopic()) || !topic.equals(config.getTopic())) {
            throw new OutboundException(OutboundErrorCode.TOPIC_MISMATCH, "ClientApi topic mismatch");
        }
        if (config.getTimeoutMs() == null || config.getTimeoutMs() < 0) {
            throw new OutboundException(OutboundErrorCode.CONFIG_MISSING, "ClientApi MQ timeoutMs must be configured");
        }
    }

    private void validateCommonRuntimeState(ClientApiRuntimeConfig config) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new OutboundException(OutboundErrorCode.CONFIG_DISABLED, "ClientApi runtime config is disabled");
        }
        if (!ACTIVE_SYNC_STATUS.equalsIgnoreCase(config.getSyncStatus())) {
            throw new OutboundException(OutboundErrorCode.CONFIG_INACTIVE, "ClientApi runtime config is not active");
        }
    }

    private boolean isHttpLike(String protocol) {
        return HTTP_PROTOCOL.equalsIgnoreCase(protocol) || WEBHOOK_PROTOCOL.equalsIgnoreCase(protocol);
    }
}
