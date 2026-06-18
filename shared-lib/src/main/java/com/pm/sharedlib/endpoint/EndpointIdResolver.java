package com.pm.sharedlib.endpoint;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class EndpointIdResolver {

    private final String serviceName;

    public EndpointIdResolver(String serviceName) {
        this.serviceName = serviceName;
    }

    public UUID resolve(String endpointKey) {
        return UUID.nameUUIDFromBytes((serviceName + ":" + endpointKey).getBytes(StandardCharsets.UTF_8));
    }
}
