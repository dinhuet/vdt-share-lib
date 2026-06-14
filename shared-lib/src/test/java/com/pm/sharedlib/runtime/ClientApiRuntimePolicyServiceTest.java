package com.pm.sharedlib.runtime;

import com.pm.sharedlib.endpoint.EndpointDefinition;
import com.pm.sharedlib.endpoint.EndpointRegistry;
import com.pm.sharedlib.endpoint.EndpointType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientApiRuntimePolicyServiceTest {

    private static final UUID ENDPOINT_ID = UUID.randomUUID();
    private static final String METHOD = "POST";
    private static final String DESTINATION_URL = "https://orders.example/api";
    private static final String TOPIC = "orders.created";

    @Mock EndpointRegistry endpointRegistry;
    @Mock SecuritySettingsStore settingsStore;

    ClientApiRuntimePolicyService service;
    EndpointDefinition endpoint;
    ClientApiRuntimeConfig config;

    @BeforeEach
    void setUp() {
        service = new ClientApiRuntimePolicyService(endpointRegistry, settingsStore);
        endpoint = EndpointDefinition.builder()
                .endpointId(ENDPOINT_ID)
                .type(EndpointType.CLIENT)
                .protocol("HTTP")
                .method(METHOD)
                .destinationUrl(DESTINATION_URL)
                .build();
        config = validConfig();
    }

    @Test
    void resolve_shouldReturnValidConfig() {
        setupEndpointAndConfig(config);

        assertThat(service.resolve(METHOD, DESTINATION_URL)).isSameAs(config);
    }

    @Test
    void resolve_shouldFailClosedWhenConfigMissing() {
        when(endpointRegistry.findClientHttp(METHOD, DESTINATION_URL)).thenReturn(Optional.of(endpoint));
        when(settingsStore.getClientApi(ENDPOINT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(METHOD, DESTINATION_URL))
                .isInstanceOf(OutboundException.class)
                .extracting("errorCode")
                .isEqualTo(OutboundErrorCode.CONFIG_MISSING);
    }

    @Test
    void resolve_shouldRejectDisabledInactiveProtocolMethodAndDestinationMismatch() {
        assertRejected(validConfig().toBuilder().enabled(false).build(), OutboundErrorCode.CONFIG_DISABLED);
        assertRejected(validConfig().toBuilder().syncStatus("STALE").build(), OutboundErrorCode.CONFIG_INACTIVE);
        assertRejected(validConfig().toBuilder().protocol("MQ").build(), OutboundErrorCode.PROTOCOL_MISMATCH);
        assertRejected(validConfig().toBuilder().protocol("WEBHOOK").build(), OutboundErrorCode.PROTOCOL_MISMATCH);
        assertRejected(validConfig().toBuilder().method("GET").build(), OutboundErrorCode.METHOD_MISMATCH);
        assertRejected(validConfig().toBuilder().destinationUrl("https://other.example/api").build(),
                OutboundErrorCode.DESTINATION_URL_MISMATCH);
    }

    @Test
    void resolve_shouldIgnoreClientIdBecauseRuntimeDtoDoesNotExposeIt() {
        setupEndpointAndConfig(config);

        assertThat(ClientApiRuntimeConfig.class.getDeclaredFields())
                .noneMatch(field -> "clientId".equals(field.getName()));
        assertThat(service.resolve(METHOD, DESTINATION_URL)).isSameAs(config);
    }

    @Test
    void resolveMq_shouldReturnValidMqConfig() {
        var mqEndpoint = EndpointDefinition.builder()
                .endpointId(ENDPOINT_ID)
                .type(EndpointType.CLIENT)
                .protocol("MQ")
                .topic(TOPIC)
                .build();
        var mqConfig = ClientApiRuntimeConfig.builder()
                .endpointId(ENDPOINT_ID)
                .protocol("MQ")
                .topic(TOPIC)
                .enabled(true)
                .syncStatus("ACTIVE")
                .timeoutMs(1000)
                .build();
        when(endpointRegistry.findClientMq(TOPIC)).thenReturn(Optional.of(mqEndpoint));
        when(settingsStore.getClientApi(ENDPOINT_ID)).thenReturn(Optional.of(mqConfig));

        assertThat(service.resolveMq(TOPIC)).isSameAs(mqConfig);
    }

    @Test
    void resolveMq_shouldRejectDisabledInactiveProtocolAndTopicMismatch() {
        assertMqRejected(validMqConfig().toBuilder().enabled(false).build(), OutboundErrorCode.CONFIG_DISABLED);
        assertMqRejected(validMqConfig().toBuilder().syncStatus("STALE").build(), OutboundErrorCode.CONFIG_INACTIVE);
        assertMqRejected(validMqConfig().toBuilder().protocol("HTTP").build(), OutboundErrorCode.PROTOCOL_MISMATCH);
        assertMqRejected(validMqConfig().toBuilder().topic("orders.other").build(), OutboundErrorCode.TOPIC_MISMATCH);
    }

    private void assertRejected(ClientApiRuntimeConfig rejectedConfig, OutboundErrorCode expectedCode) {
        setupEndpointAndConfig(rejectedConfig);

        assertThatThrownBy(() -> service.resolve(METHOD, DESTINATION_URL))
                .isInstanceOf(OutboundException.class)
                .extracting("errorCode")
                .isEqualTo(expectedCode);
    }

    private void setupEndpointAndConfig(ClientApiRuntimeConfig runtimeConfig) {
        when(endpointRegistry.findClientHttp(METHOD, DESTINATION_URL)).thenReturn(Optional.of(endpoint));
        when(settingsStore.getClientApi(ENDPOINT_ID)).thenReturn(Optional.of(runtimeConfig));
    }

    private void assertMqRejected(ClientApiRuntimeConfig rejectedConfig, OutboundErrorCode expectedCode) {
        var mqEndpoint = EndpointDefinition.builder()
                .endpointId(ENDPOINT_ID)
                .type(EndpointType.CLIENT)
                .protocol("MQ")
                .topic(TOPIC)
                .build();
        when(endpointRegistry.findClientMq(TOPIC)).thenReturn(Optional.of(mqEndpoint));
        when(settingsStore.getClientApi(ENDPOINT_ID)).thenReturn(Optional.of(rejectedConfig));

        assertThatThrownBy(() -> service.resolveMq(TOPIC))
                .isInstanceOf(OutboundException.class)
                .extracting("errorCode")
                .isEqualTo(expectedCode);
    }

    private ClientApiRuntimeConfig validConfig() {
        return ClientApiRuntimeConfig.builder()
                .id(UUID.randomUUID())
                .endpointId(ENDPOINT_ID)
                .protocol("HTTP")
                .method(METHOD)
                .destinationUrl(DESTINATION_URL)
                .enabled(true)
                .syncStatus("ACTIVE")
                .build();
    }

    private ClientApiRuntimeConfig validMqConfig() {
        return ClientApiRuntimeConfig.builder()
                .id(UUID.randomUUID())
                .endpointId(ENDPOINT_ID)
                .protocol("MQ")
                .topic(TOPIC)
                .enabled(true)
                .syncStatus("ACTIVE")
                .timeoutMs(1000)
                .build();
    }
}
