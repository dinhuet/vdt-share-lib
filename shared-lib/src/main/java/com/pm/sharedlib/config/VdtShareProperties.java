package com.pm.sharedlib.config;

import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "vdt.share")
public class VdtShareProperties {
    boolean enabled = true;
    String endpointManifestPath = "./.vdt-share/${spring.application.name}/endpoints.json";
    Runtime runtime = new Runtime();

    @Data
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    public static class Runtime {
        boolean failOpen = false;
        String exposedApiKeyPrefix = "vdt:exposed-api";
        String accessPolicyKeyPrefix = "vdt:access-policy";
        String keyIdPrefix = "vdt:key-id";
        String clientPermissionKeyPrefix = "vdt:client-permission";
    }
}
