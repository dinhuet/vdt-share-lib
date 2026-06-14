package com.pm.sharedlib.endpoint;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EndpointRegistryTest {

    @Test
    void findExposedMqByTopic_shouldReturnMqEndpointByTopicOnly() {
        var endpoint = exposedMq("orders.created", "ListenerA", "handle");
        var registry = initializedRegistry(List.of(endpoint));

        var result = registry.findExposedMqByTopic("orders.created");

        assertThat(result).isPresent();
        assertThat(result.get().getHandlerClass()).isEqualTo("ListenerA");
    }

    @Test
    void findExposedMqByTopic_shouldIgnoreBlankMissingAndHttpEndpoints() {
        var httpEndpoint = EndpointDefinition.builder()
                .type(EndpointType.EXPOSED)
                .protocol("HTTP")
                .name("http-api")
                .method("GET")
                .path("/orders")
                .build();
        var registry = initializedRegistry(List.of(httpEndpoint));

        assertThat(registry.findExposedMqByTopic(null)).isEmpty();
        assertThat(registry.findExposedMqByTopic(" ")).isEmpty();
        assertThat(registry.findExposedMqByTopic("orders.created")).isEmpty();
    }

    @Test
    void findClientMq_shouldReturnMqClientEndpointByTopic() {
        var clientMq = EndpointDefinition.builder()
                .type(EndpointType.CLIENT)
                .protocol("MQ")
                .name("publish-orders")
                .topic("orders.created")
                .build();
        var registry = initializedRegistry(List.of(), List.of(clientMq));

        assertThat(registry.findClientMq("orders.created")).isPresent();
        assertThat(registry.findClientMq(" ")).isEmpty();
        assertThat(registry.findClientMq("orders.other")).isEmpty();
    }

    @Test
    void initialize_shouldRejectDuplicateNonBlankExposedMqTopics() {
        var scanner = mock(EndpointScanner.class);
        var manifestStore = mock(EndpointManifestStore.class);
        when(manifestStore.read()).thenReturn(Optional.empty());
        when(scanner.scan()).thenReturn(new EndpointScanner.ScannedEndpoints(List.of(
                exposedMq("orders.created", "ListenerA", "handleA"),
                exposedMq("orders.created", "ListenerB", "handleB")), List.of()));
        var registry = new EndpointRegistry(scanner, manifestStore);

        assertThatThrownBy(() -> registry.initialize("test-service"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate exposed MQ topic: orders.created");
    }

    private EndpointRegistry initializedRegistry(List<EndpointDefinition> exposedApis) {
        return initializedRegistry(exposedApis, List.of());
    }

    private EndpointRegistry initializedRegistry(List<EndpointDefinition> exposedApis, List<EndpointDefinition> clientApis) {
        var scanner = mock(EndpointScanner.class);
        var manifestStore = mock(EndpointManifestStore.class);
        when(manifestStore.read()).thenReturn(Optional.empty());
        when(scanner.scan()).thenReturn(new EndpointScanner.ScannedEndpoints(exposedApis, clientApis));
        var registry = new EndpointRegistry(scanner, manifestStore);
        registry.initialize("test-service");
        return registry;
    }

    private EndpointDefinition exposedMq(String topic, String handlerClass, String handlerMethod) {
        return EndpointDefinition.builder()
                .type(EndpointType.EXPOSED)
                .protocol("MQ")
                .name(handlerClass + "#" + handlerMethod)
                .topic(topic)
                .handlerClass(handlerClass)
                .handlerMethod(handlerMethod)
                .build();
    }
}
