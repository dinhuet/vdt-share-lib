package com.pm.sharedlib.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceRegistrationEvent {
    String eventType;
    String serviceName;
    String serviceUrl;
    String keyService;
    List<ApiInfo> exposedApis;
    List<ApiInfo> clientApis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ApiInfo {
        String name;
        String path;
        String destinationUrl;
        String method;
        String protocol;
    }
}
