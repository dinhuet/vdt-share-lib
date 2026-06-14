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
        boolean httpFilterEnabled = true;
        boolean mqInterceptorEnabled = true;
        boolean failOpen = false;
        boolean hmacEnabled = false;
        long hmacMaxClockSkewSeconds = 300;
        String credentialEncryptionKey;
        String exposedApiKeyPrefix = "vdt:exposed-api";
        String clientApiKeyPrefix = "vdt:client-api";
        String accessPolicyKeyPrefix = "vdt:access-policy";
        String keyIdPrefix = "vdt:key-id";
        String clientPermissionKeyPrefix = "vdt:client-permission";
        String rateLimitKeyPrefix = "vdt:rate-limit";
        String nonceKeyPrefix = "vdt:hmac-nonce";
    }
}
