package com.pm.sharedlib.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceRegistrationEvent {
    String eventType;
    String serviceName;
    String serviceUrl;
    List<ApiInfo> exposedApis;
    List<ApiInfo> clientApis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ApiInfo {
        UUID endpointId;
        String endpointKey;
        String name;
        String path;
        String destinationUrl;
        String topic;
        String method;
        String protocol;
    }
}
