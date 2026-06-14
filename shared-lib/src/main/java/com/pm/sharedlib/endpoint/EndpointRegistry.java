package com.pm.sharedlib.endpoint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class EndpointRegistry {

    private final EndpointScanner scanner;
    private final EndpointManifestStore manifestStore;
    private final PathPatternParser pathPatternParser = new PathPatternParser();
    private final List<EndpointDefinition> exposedApis = new ArrayList<>();
    private final List<EndpointDefinition> clientApis = new ArrayList<>();
    private boolean initialized;

    public EndpointRegistry(EndpointScanner scanner, EndpointManifestStore manifestStore) {
        this.scanner = scanner;
        this.manifestStore = manifestStore;
    }

    public synchronized void initialize(String serviceName) {
        if (initialized) {
            return;
        }

        var previousManifest = manifestStore.read().orElse(null);
        var resolver = new EndpointIdResolver(previousManifest);
        var scanned = scanner.scan();

        var resolvedExposedApis = resolveEndpointIds(scanned.exposedApis(), resolver);
        var resolvedClientApis = resolveEndpointIds(scanned.clientApis(), resolver);
        validate(resolvedExposedApis, resolvedClientApis);

        exposedApis.clear();
        exposedApis.addAll(resolvedExposedApis);
        clientApis.clear();
        clientApis.addAll(resolvedClientApis);

        manifestStore.write(EndpointManifest.builder()
                .version(1)
                .serviceName(serviceName)
                .generatedAt(LocalDateTime.now().toString())
                .exposedApis(resolvedExposedApis)
                .clientApis(resolvedClientApis)
                .build());

        initialized = true;
        log.info("Initialized endpoint registry: exposedApis={}, clientApis={}", exposedApis.size(), clientApis.size());
    }

    public List<EndpointDefinition> getExposedApis() {
        return List.copyOf(exposedApis);
    }

    public List<EndpointDefinition> getClientApis() {
        return List.copyOf(clientApis);
    }

    public Optional<EndpointDefinition> findExposedHttp(String method, String requestPath) {
        var normalizedMethod = method == null ? "" : method.toUpperCase();
        var path = PathContainer.parsePath(requestPath == null ? "" : requestPath);
        return exposedApis.stream()
                .filter(endpoint -> "HTTP".equalsIgnoreCase(endpoint.getProtocol()))
                .filter(endpoint -> normalizedMethod.equalsIgnoreCase(endpoint.getMethod()))
                .filter(endpoint -> endpoint.getPath() != null)
                .filter(endpoint -> pathPatternParser.parse(endpoint.getPath()).matches(path))
                .findFirst();
    }

    public Optional<EndpointDefinition> findExposedMq(String topic, String handlerClass, String handlerMethod) {
        return exposedApis.stream()
                .filter(endpoint -> "MQ".equalsIgnoreCase(endpoint.getProtocol()))
                .filter(endpoint -> topic != null && topic.equals(endpoint.getTopic()))
                .filter(endpoint -> handlerClass != null && handlerClass.equals(endpoint.getHandlerClass()))
                .filter(endpoint -> handlerMethod != null && handlerMethod.equals(endpoint.getHandlerMethod()))
                .findFirst();
    }

    public Optional<EndpointDefinition> findExposedMqByTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return Optional.empty();
        }
        return exposedApis.stream()
                .filter(endpoint -> "MQ".equalsIgnoreCase(endpoint.getProtocol()))
                .filter(endpoint -> topic.equals(endpoint.getTopic()))
                .findFirst();
    }

    public Optional<EndpointDefinition> findClientHttp(String method, String destinationUrl) {
        var normalizedMethod = method == null ? "" : method.toUpperCase();
        return clientApis.stream()
                .filter(endpoint -> "HTTP".equalsIgnoreCase(endpoint.getProtocol())
                        || "WEBHOOK".equalsIgnoreCase(endpoint.getProtocol()))
                .filter(endpoint -> normalizedMethod.equalsIgnoreCase(endpoint.getMethod()))
                .filter(endpoint -> destinationUrl != null && destinationUrl.equals(endpoint.getDestinationUrl()))
                .findFirst();
    }

    public Optional<EndpointDefinition> findClientMq(String topic) {
        if (topic == null || topic.isBlank()) {
            return Optional.empty();
        }
        return clientApis.stream()
                .filter(endpoint -> "MQ".equalsIgnoreCase(endpoint.getProtocol()))
                .filter(endpoint -> topic.equals(endpoint.getTopic()))
                .findFirst();
    }

    private List<EndpointDefinition> resolveEndpointIds(List<EndpointDefinition> endpoints, EndpointIdResolver resolver) {
        return endpoints.stream()
                .map(endpoint -> {
                    var endpointKey = buildEndpointKey(endpoint);
                    var endpointId = resolver.resolve(endpointKey);
                    return EndpointDefinition.builder()
                            .endpointId(endpointId)
                            .endpointKey(endpointKey)
                            .type(endpoint.getType())
                            .protocol(endpoint.getProtocol())
                            .name(endpoint.getName())
                            .method(endpoint.getMethod())
                            .path(endpoint.getPath())
                            .destinationUrl(endpoint.getDestinationUrl())
                            .topic(endpoint.getTopic())
                            .handlerClass(endpoint.getHandlerClass())
                            .handlerMethod(endpoint.getHandlerMethod())
                            .build();
                })
                .toList();
    }

    private String buildEndpointKey(EndpointDefinition endpoint) {
        if (endpoint.getType() == EndpointType.EXPOSED && "MQ".equalsIgnoreCase(endpoint.getProtocol())) {
            return EndpointKeyFactory.exposedMq(endpoint.getTopic(), endpoint.getHandlerClass(), endpoint.getHandlerMethod());
        }
        if (endpoint.getType() == EndpointType.EXPOSED) {
            return EndpointKeyFactory.exposedHttp(endpoint.getMethod(), endpoint.getPath());
        }
        if ("MQ".equalsIgnoreCase(endpoint.getProtocol())) {
            return EndpointKeyFactory.clientMq(endpoint.getTopic());
        }
        return EndpointKeyFactory.clientHttp(endpoint.getMethod(), endpoint.getDestinationUrl());
    }

    private void validate(List<EndpointDefinition> resolvedExposedApis, List<EndpointDefinition> resolvedClientApis) {
        var keys = new HashSet<String>();
        var ids = new HashSet<UUID>();
        var exposedMqTopics = new HashMap<String, String>();
        for (var endpoint : concat(resolvedExposedApis, resolvedClientApis)) {
            if (endpoint.getName() == null || endpoint.getName().isBlank()) {
                throw new IllegalStateException("Endpoint name must not be blank: " + endpoint.getEndpointKey());
            }
            if (!keys.add(endpoint.getEndpointKey())) {
                throw new IllegalStateException("Duplicate endpointKey: " + endpoint.getEndpointKey());
            }
            if (!ids.add(endpoint.getEndpointId())) {
                throw new IllegalStateException("Duplicate endpointId: " + endpoint.getEndpointId());
            }
            if (endpoint.getType() == EndpointType.EXPOSED
                    && "MQ".equalsIgnoreCase(endpoint.getProtocol())
                    && endpoint.getTopic() != null
                    && !endpoint.getTopic().isBlank()) {
                var previousKey = exposedMqTopics.putIfAbsent(endpoint.getTopic(), endpoint.getEndpointKey());
                if (previousKey != null) {
                    throw new IllegalStateException("Duplicate exposed MQ topic: " + endpoint.getTopic()
                            + " (" + previousKey + ", " + endpoint.getEndpointKey() + ")");
                }
            }
        }
    }

    private List<EndpointDefinition> concat(List<EndpointDefinition> first, List<EndpointDefinition> second) {
        var result = new ArrayList<EndpointDefinition>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }
}
