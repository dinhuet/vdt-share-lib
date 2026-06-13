package com.pm.sharedlib.runtime;

import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;

public class KafkaRuntimeAuthHeaders implements RuntimeAuthHeaders {

    private final Headers headers;

    public KafkaRuntimeAuthHeaders(Headers headers) {
        this.headers = headers;
    }

    @Override
    public String get(String name) {
        var header = headers.lastHeader(name);
        if (header == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
