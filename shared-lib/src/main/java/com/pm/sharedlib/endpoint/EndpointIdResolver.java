package com.pm.sharedlib.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EndpointIdResolver {

    private final Map<String, UUID> endpointIdsByKey;

    public EndpointIdResolver(EndpointManifest previousManifest) {
        this.endpointIdsByKey = new HashMap<>();
        if (previousManifest != null) {
            addAll(previousManifest.getExposedApis());
            addAll(previousManifest.getClientApis());
        }
    }

    public UUID resolve(String endpointKey) {
        return endpointIdsByKey.computeIfAbsent(endpointKey, ignored -> UUID.randomUUID());
    }

    private void addAll(List<EndpointDefinition> endpoints) {
        if (endpoints == null) {
            return;
        }
        for (var endpoint : endpoints) {
            if (endpoint.getEndpointKey() != null && endpoint.getEndpointId() != null) {
                endpointIdsByKey.put(endpoint.getEndpointKey(), endpoint.getEndpointId());
            }
        }
    }
}
